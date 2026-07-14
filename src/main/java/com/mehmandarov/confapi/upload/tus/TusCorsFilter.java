package com.mehmandarov.confapi.upload.tus;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * CORS headers for the TUS endpoints, so browser clients (tus-js-client) can
 * talk to them cross-origin. Without {@code Access-Control-Expose-Headers} the
 * browser hides {@code Location} and {@code Upload-Offset} from JavaScript and
 * the client cannot resume.
 */
@Provider
public class TusCorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // Only for the TUS demo endpoints – don't leak CORS headers elsewhere
        if (!request.getUriInfo().getPath().startsWith("tus")) {
            return;
        }
        response.getHeaders().add("Access-Control-Allow-Origin", "*"); // narrow this in production
        response.getHeaders().add("Access-Control-Allow-Methods", "POST, HEAD, PATCH, DELETE, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers",
                "Tus-Resumable, Upload-Length, Upload-Offset, Upload-Metadata, Content-Type");
        response.getHeaders().add("Access-Control-Expose-Headers",
                "Tus-Resumable, Upload-Offset, Location");
    }
}
