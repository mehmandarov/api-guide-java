package com.mehmandarov.confapi.upload.tus;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resumable uploads with the TUS protocol - pure {@code jakarta.ws.rs}.
 * <p>
 * The TUS core protocol (<a href="https://tus.io/protocols/resumable-upload">spec</a>)
 * in three requests:
 * <ul>
 *   <li>{@code POST /api/tus} - create an upload; returns {@code Location}.</li>
 *   <li>{@code HEAD /api/tus/{id}} - ask where to resume; returns {@code Upload-Offset}.</li>
 *   <li>{@code PATCH /api/tus/{id}} - append bytes at that offset.</li>
 * </ul>
 * The file on disk is the single source of truth for how many bytes have
 * arrived; a mismatching {@code Upload-Offset} gets a {@code 409 Conflict}.
 * Companion code for the blog post "Resumable file uploads with TUS and
 * Jakarta EE". Demo-only: the in-memory metadata map does not survive restarts.
 */
@Path("/tus")
@Tag(name = "Demos", description = "Standalone demos referenced from the blog")
@PermitAll
@RequestScoped
public class TusResource {

    private static final String TUS_VERSION = "1.0.0";
    private static final java.nio.file.Path STORAGE_DIR =
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "tus-uploads");
    private static final java.nio.file.Path META_DIR = STORAGE_DIR.resolve(".meta");

    // upload id -> declared total length
    private static final Map<String, Long> UPLOAD_LENGTHS = new ConcurrentHashMap<>();
    // upload id -> sanitized original filename (from Upload-Metadata), if any
    private static final Map<String, String> UPLOAD_NAMES = new ConcurrentHashMap<>();
    // upload id -> current on-disk path (moves from ".part" to final name on completion)
    private static final Map<String, java.nio.file.Path> UPLOAD_PATHS = new ConcurrentHashMap<>();

    @Context
    UriInfo uriInfo;

    // Capability discovery – what the server supports
    @OPTIONS
    @Operation(summary = "TUS capability discovery")
    @APIResponse(responseCode = "204", description = "Supported TUS version and extensions in headers")
    public Response options() {
        return Response.noContent()
                .header("Tus-Resumable", TUS_VERSION)
                .header("Tus-Version", TUS_VERSION)
                .header("Tus-Extension", "creation,termination")
                .build();
    }

    // 1. Create the upload – returns the URL the client will PATCH to
    @POST
    @Operation(summary = "Create a new TUS upload")
    @APIResponse(responseCode = "201", description = "Upload created; PATCH to the Location URL")
    @APIResponse(responseCode = "400", description = "Missing or invalid Upload-Length header")
    public Response create(@HeaderParam("Upload-Length") Long uploadLength,
                           @HeaderParam("Upload-Metadata") String uploadMetadata) throws IOException {
        if (uploadLength == null || uploadLength < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .header("Tus-Resumable", TUS_VERSION)
                    .build();
        }

        String id = UUID.randomUUID().toString();
        Files.createDirectories(STORAGE_DIR);
        Files.createDirectories(META_DIR);

        // The TUS "filename" metadata key is base64-encoded per the spec:
        //   Upload-Metadata: filename <base64>, filetype <base64>
        // If the client supplies one, we open the file on disk with a
        // human-readable name from the start ({shortId}-{name}.part) so
        // partial uploads are easy to identify without a map lookup.
        // Falls back to the opaque UUID for clients that don't send it.
        String originalName = parseMetadata(uploadMetadata).get("filename");
        String cleanName = (originalName != null) ? sanitizeFilename(originalName) : null;
        java.nio.file.Path file = pathFor(id, cleanName, /*complete*/ false);
        Files.createFile(file);
        UPLOAD_LENGTHS.put(id, uploadLength);
        UPLOAD_PATHS.put(id, file);
        if (cleanName != null) UPLOAD_NAMES.put(id, cleanName);

        // Persist metadata alongside the file so the maps can be rebuilt
        // after a class reload (Quarkus dev mode) or a container restart.
        // Without this, every code change here would orphan every partial
        // upload on disk and the resume-after-refresh flow would silently
        // start over from byte 0.
        saveMeta(id, uploadLength, cleanName);

        URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
        return Response.created(location)
                .header("Tus-Resumable", TUS_VERSION)
                .build();
    }

    // 2. Ask where to resume from
    @HEAD
    @Path("/{id}")
    @Operation(summary = "Get the current offset of a TUS upload")
    @APIResponse(responseCode = "204", description = "Current offset in the Upload-Offset header")
    @APIResponse(responseCode = "404", description = "Unknown upload id")
    public Response offset(@PathParam("id") String id) throws IOException {
        if (!ensureLoaded(id)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .header("Tus-Resumable", TUS_VERSION)
                    .build();
        }

        long offset = Files.size(UPLOAD_PATHS.get(id));
        return Response.noContent()
                .header("Tus-Resumable", TUS_VERSION)
                .header("Upload-Offset", offset)
                .header("Upload-Length", UPLOAD_LENGTHS.get(id))
                .header("Cache-Control", "no-store")
                .build();
    }

    // 3. Append a chunk at the given offset
    @PATCH
    @Path("/{id}")
    @Consumes("application/offset+octet-stream")
    @Operation(summary = "Append a chunk to a TUS upload")
    @APIResponse(responseCode = "204", description = "Chunk appended; new offset in the Upload-Offset header")
    @APIResponse(responseCode = "404", description = "Unknown upload id")
    @APIResponse(responseCode = "409", description = "Upload-Offset does not match the bytes on disk")
    public Response append(@PathParam("id") String id,
                           @HeaderParam("Upload-Offset") Long uploadOffset,
                           InputStream body) throws IOException {
        if (!ensureLoaded(id)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .header("Tus-Resumable", TUS_VERSION)
                    .build();
        }
        long length = UPLOAD_LENGTHS.get(id);
        java.nio.file.Path file = UPLOAD_PATHS.get(id);
        long currentOffset = Files.size(file);

        // The client's idea of the offset must match ours – otherwise 409
        if (uploadOffset == null || uploadOffset != currentOffset) {
            return Response.status(Response.Status.CONFLICT)
                    .header("Tus-Resumable", TUS_VERSION)
                    .header("Upload-Offset", currentOffset)
                    .build();
        }

        // TUS spec: the server MUST NOT accept more bytes than Upload-Length.
        // Cap the copy at the remaining space and silently drain any excess
        // (defends against clients whose File source returns more bytes than
        // its declared size - a real cause of "upload was configured with a
        // size of N bytes, but the source is done after N+k bytes" errors).
        long remaining = length - currentOffset;
        long newOffset;
        try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
            copyAtMost(body, out, remaining);
            drain(body);
            newOffset = Files.size(file);
        }

        // Upload complete: drop the ".part" suffix so the directory listing
        // reflects finished vs in-progress at a glance, and remove the
        // sidecar meta file (no longer needed once the upload is final).
        if (newOffset == length) {
            String name = UPLOAD_NAMES.get(id);
            if (name != null) {
                java.nio.file.Path finalPath = pathFor(id, name, /*complete*/ true);
                Files.move(file, finalPath);
                UPLOAD_PATHS.put(id, finalPath);
            }
            Files.deleteIfExists(metaPath(id));
        }

        return Response.noContent()
                .header("Tus-Resumable", TUS_VERSION)
                .header("Upload-Offset", newOffset)
                .build();
    }

    // 4. Cancel: TUS Termination extension. tus-js-client's abort(true) hits this.
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Terminate a TUS upload and delete its bytes")
    @APIResponse(responseCode = "204", description = "Upload terminated; server state cleared")
    @APIResponse(responseCode = "404", description = "Unknown upload id")
    public Response terminate(@PathParam("id") String id) throws IOException {
        if (!ensureLoaded(id)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .header("Tus-Resumable", TUS_VERSION)
                    .build();
        }
        java.nio.file.Path file = UPLOAD_PATHS.remove(id);
        UPLOAD_LENGTHS.remove(id);
        UPLOAD_NAMES.remove(id);
        if (file != null) Files.deleteIfExists(file);
        Files.deleteIfExists(metaPath(id));
        return Response.noContent()
                .header("Tus-Resumable", TUS_VERSION)
                .build();
    }

    // 5. Demo-only: list every file currently in the storage directory.
    // Emits tiny hand-rolled JSON (no JSON-B dependency here) so the demo
    // page can render a "Files on the server" table. Do NOT expose in prod.
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Demo-only: list all uploads on the server")
    @APIResponse(responseCode = "200", description = "Array of {name,size,inProgress,length} objects")
    public Response listAll() throws IOException {
        StringBuilder json = new StringBuilder("[");
        if (Files.isDirectory(STORAGE_DIR)) {
            boolean first = true;
            try (var stream = Files.list(STORAGE_DIR)) {
                var files = stream.filter(Files::isRegularFile).sorted().toList();
                for (var p : files) {
                    String name = p.getFileName().toString();
                    long size = Files.size(p);
                    boolean inProgress = name.endsWith(".part");
                    // For in-progress files, look up the declared length via
                    // the sidecar meta so the UI can show a progress %.
                    Long length = inProgress ? declaredLengthFor(name) : null;
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"name\":\"").append(jsonEscape(name)).append("\",")
                        .append("\"size\":").append(size).append(',')
                        .append("\"inProgress\":").append(inProgress).append(',')
                        .append("\"length\":").append(length == null ? "null" : length)
                        .append('}');
                }
            }
        }
        json.append(']');
        return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
    }

    // 6. Demo-only: wipe every upload (data + sidecar meta + in-memory maps).
    // Not part of the TUS spec - handy for the browser demo's "Purge server"
    // button so you can reset between recordings without exec'ing into the
    // container. Do NOT expose this in production.
    @DELETE
    @Operation(summary = "Demo-only: purge ALL uploads on the server")
    @APIResponse(responseCode = "200", description = "Number of files removed")
    public Response purgeAll() throws IOException {        UPLOAD_LENGTHS.clear();
        UPLOAD_NAMES.clear();
        UPLOAD_PATHS.clear();

        int removed = 0;
        if (Files.isDirectory(STORAGE_DIR)) {
            try (var stream = Files.list(STORAGE_DIR)) {
                for (var p : (Iterable<java.nio.file.Path>) stream::iterator) {
                    if (Files.isRegularFile(p)) {
                        Files.deleteIfExists(p);
                        removed++;
                    }
                }
            }
        }
        if (Files.isDirectory(META_DIR)) {
            try (var stream = Files.list(META_DIR)) {
                for (var p : (Iterable<java.nio.file.Path>) stream::iterator) {
                    Files.deleteIfExists(p);
                }
            }
        }
        return Response.ok("{\"removed\":" + removed + "}", MediaType.APPLICATION_JSON).build();
    }

    // ---------- helpers ----------

    /** Build the on-disk path for an upload, with the {@code .part} suffix while incomplete. */
    private static java.nio.file.Path pathFor(String id, String sanitizedName, boolean complete) {
        if (sanitizedName == null) return STORAGE_DIR.resolve(id);
        String suffix = complete ? "" : ".part";
        return STORAGE_DIR.resolve(id.substring(0, 8) + "-" + sanitizedName + suffix);
    }

    private static java.nio.file.Path metaPath(String id) {
        return META_DIR.resolve(id);
    }

    /** Persist minimum metadata to survive JVM / container / dev-mode restarts. */
    private static void saveMeta(String id, long length, String sanitizedName) throws IOException {
        Files.createDirectories(META_DIR);
        String body = "length=" + length + "\n"
                    + "name=" + (sanitizedName == null ? "" : sanitizedName) + "\n";
        Files.writeString(metaPath(id), body, StandardCharsets.UTF_8);
    }

    /**
     * Ensure the in-memory maps have an entry for {@code id}. Rebuilds them
     * from the sidecar meta file (and the file on disk) if necessary. Returns
     * {@code false} if the upload is unknown - i.e. no meta file exists.
     */
    private static boolean ensureLoaded(String id) throws IOException {
        if (UPLOAD_LENGTHS.containsKey(id) && UPLOAD_PATHS.containsKey(id)) return true;

        java.nio.file.Path meta = metaPath(id);
        if (!Files.exists(meta)) return false;

        Map<String, String> kv = new HashMap<>();
        for (String line : Files.readAllLines(meta, StandardCharsets.UTF_8)) {
            int eq = line.indexOf('=');
            if (eq > 0) kv.put(line.substring(0, eq), line.substring(eq + 1));
        }
        long length;
        try {
            length = Long.parseLong(kv.getOrDefault("length", "-1"));
        } catch (NumberFormatException e) {
            return false;
        }
        if (length < 0) return false;
        String name = kv.getOrDefault("name", "");
        if (name.isEmpty()) name = null;

        // If both a completed and a .part file exist (shouldn't happen, but be safe), prefer .part
        java.nio.file.Path partPath  = pathFor(id, name, false);
        java.nio.file.Path finalPath = pathFor(id, name, true);
        java.nio.file.Path file;
        if (Files.exists(partPath)) file = partPath;
        else if (Files.exists(finalPath)) file = finalPath;
        else return false;

        UPLOAD_LENGTHS.put(id, length);
        UPLOAD_PATHS.put(id, file);
        if (name != null) UPLOAD_NAMES.put(id, name);
        return true;
    }

    /** Parse "key1 base64Value1,key2 base64Value2,..." into a decoded map. */
    private static Map<String, String> parseMetadata(String header) {
        Map<String, String> out = new HashMap<>();
        if (header == null || header.isBlank()) return out;
        for (String pair : header.split(",")) {
            String[] kv = pair.trim().split(" ", 2);
            if (kv.length == 2) {
                try {
                    out.put(kv[0], new String(Base64.getDecoder().decode(kv[1]), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ignored) {
                    // malformed base64 - skip this entry
                }
            }
        }
        return out;
    }

    /** Strip path separators and dodgy characters; keep it safe for {@code STORAGE_DIR.resolve(...)}. */
    private static String sanitizeFilename(String raw) {
        String base = raw.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) cleaned = "upload";
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    /** Copy up to {@code max} bytes from {@code in} to {@code out}. */
    private static void copyAtMost(InputStream in, OutputStream out, long max) throws IOException {
        byte[] buf = new byte[8192];
        long left = max;
        while (left > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (n < 0) return;
            out.write(buf, 0, n);
            left -= n;
        }
    }

    /** Drain and discard anything still available on {@code in}. */
    private static void drain(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        do {
            n = in.read(buf);
        } while (n >= 0);
    }

    /**
     * Given an in-progress filename like {@code "a8e2ac6d-myfile.bin.part"},
     * find the matching sidecar meta file and return its declared length,
     * or {@code null} if we can't figure it out. Used by {@link #listAll()}
     * so the UI can show a "45 / 100 MB" progress hint.
     */
    private static Long declaredLengthFor(String partFilename) {
        int dash = partFilename.indexOf('-');
        if (dash != 8) return null;                        // shape: "{shortId}-...part"
        String shortId = partFilename.substring(0, dash);
        if (!Files.isDirectory(META_DIR)) return null;
        try (var stream = Files.list(META_DIR)) {
            var match = stream.filter(p -> p.getFileName().toString().startsWith(shortId + "-"))
                              .findFirst();
            if (match.isEmpty()) return null;
            for (String line : Files.readAllLines(match.get(), StandardCharsets.UTF_8)) {
                if (line.startsWith("length=")) {
                    return Long.parseLong(line.substring("length=".length()));
                }
            }
        } catch (IOException | NumberFormatException ignored) { /* fall through */ }
        return null;
    }

    /** Minimal JSON string escape - enough for the filename characters we emit. */
    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
