package com.mehmandarov.confapi.versioning;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * <strong>The Evolution — Header-based Version Router</strong>
 * <p>
 * A {@code @PreMatching} filter that supports <em>header-based</em> API
 * versioning alongside the existing URI-based versioning.
 * <p>
 * Clients can request a specific API version via:
 * <ul>
 *   <li>Header: {@code X-API-Version: 2}</li>
 *   <li>Accept header parameter: {@code application/json; version=2}</li>
 * </ul>
 * <p>
 * When a version header is detected and the request URI does NOT already
 * contain a version prefix ({@code /v1/}, {@code /v2/}), this filter
 * rewrites the URI to include the correct version prefix.
 * <p>
 * This means clients can call {@code /api/sessions} with an
 * {@code X-API-Version: 2} header and be transparently routed to the
 * V2 resource at {@code /api/v2/sessions}.
 */
@Provider
@PreMatching
public class HeaderVersionFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(HeaderVersionFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        // In @PreMatching, getPath() returns the path relative to the base URI
        // (e.g. "sessions" or "v1/sessions"). Some runtimes may include a leading slash.
        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Skip if the URI already contains a version prefix
        if (path.matches("v\\d+(/.*)?")) {
            return;
        }

        String version = detectVersion(ctx);
        if (version != null) {
            String newPath = "v" + version + "/" + path;
            URI baseUri = ctx.getUriInfo().getBaseUri();
            URI requestUri = ctx.getUriInfo().getRequestUri();

            // Reconstruct the full absolute request URI with the versioned path
            String newAbsolute = baseUri.toString();
            if (!newAbsolute.endsWith("/")) {
                newAbsolute += "/";
            }
            newAbsolute += newPath;
            if (requestUri.getRawQuery() != null) {
                newAbsolute += "?" + requestUri.getRawQuery();
            }

            URI newUri = URI.create(newAbsolute);

            LOG.info(() -> String.format(
                    "[VERSIONING] Rewriting %s → %s (version=%s)",
                    ctx.getUriInfo().getRequestUri(), newUri, version));

            ctx.setRequestUri(baseUri, newUri);
        }
    }

    /**
     * Detect the requested API version from headers.
     *
     * @return the version string (e.g. "1", "2") or null if not specified
     */
    private String detectVersion(ContainerRequestContext ctx) {
        // 1. Check X-API-Version header
        String header = ctx.getHeaderString("X-API-Version");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }

        // 2. Check Accept header for version parameter
        //    e.g. Accept: application/json; version=2
        String accept = ctx.getHeaderString("Accept");
        if (accept != null) {
            for (String part : accept.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("version=")) {
                    return trimmed.substring("version=".length()).trim();
                }
            }
        }

        return null;
    }
}
