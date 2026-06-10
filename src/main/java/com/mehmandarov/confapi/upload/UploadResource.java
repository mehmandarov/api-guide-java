package com.mehmandarov.confapi.upload;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Receiving binary, the portable way - pure {@code jakarta.ws.rs}.
 * <p>
 * Two endpoints:
 * <ul>
 *   <li>{@code POST /api/uploads/multipart} - a {@code multipart/form-data}
 *       request with a text part and a binary part, read via the standard
 *       Jakarta REST {@link EntityPart} API (Jakarta REST 3.1+).</li>
 *   <li>{@code POST /api/uploads/raw} - a single binary body
 *       ({@code application/octet-stream}); the body <em>is</em> the file.</li>
 * </ul>
 * Both stream the bytes rather than buffering them. Companion code for the blog
 * post "Receiving binary: REST endpoints that take file uploads".
 */
@Path("/uploads")
@Tag(name = "Demos", description = "Standalone demos referenced from the blog")
@PermitAll
@RequestScoped
public class UploadResource {

    @POST
    @Path("/multipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Accept a file plus a text description as multipart/form-data")
    @APIResponse(responseCode = "200", description = "Summary of what was received")
    public Response upload(List<EntityPart> parts) throws IOException {
        String description = null;
        String fileName = null;
        long bytesReceived = 0;

        for (EntityPart part : parts) {
            switch (part.getName()) {
                case "description" -> description = part.getContent(String.class);
                case "file" -> {
                    fileName = part.getFileName().orElse("unnamed");
                    try (InputStream in = part.getContent()) {
                        bytesReceived = in.transferTo(OutputStream.nullOutputStream());
                        // in real code: stream to storage, scan, resize, etc.
                    }
                }
                default -> { /* ignore unknown parts */ }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fileName", fileName);
        summary.put("bytes", bytesReceived);
        summary.put("description", description == null ? "" : description);
        return Response.ok(summary).build();
    }

    @POST
    @Path("/raw")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Accept a single binary body; metadata travels in headers")
    @APIResponse(responseCode = "200", description = "Summary of what was received")
    public Response uploadRaw(
            InputStream body,
            @HeaderParam("Content-Type") String contentType,
            @HeaderParam("X-File-Name") String fileName) throws IOException {

        long bytes = body.transferTo(OutputStream.nullOutputStream());
        // in real code: stream to storage instead of counting

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fileName", fileName == null ? "unnamed" : fileName);
        summary.put("contentType", contentType == null ? "application/octet-stream" : contentType);
        summary.put("bytes", bytes);
        return Response.ok(summary).build();
    }
}

