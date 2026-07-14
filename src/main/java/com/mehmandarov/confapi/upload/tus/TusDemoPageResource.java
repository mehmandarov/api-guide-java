package com.mehmandarov.confapi.upload.tus;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.InputStream;
import java.net.URI;

/**
 * Serves the tus-js-client demo page and its stylesheet at {@code /api/tus/demo/}
 * (HTML) and {@code /api/tus/demo/tus-upload-demo.css} (CSS), so the browser
 * demo is available at runtime on every supported runtime (Quarkus,
 * Open Liberty, Helidon) - each has a different convention for static
 * assets, but they all speak JAX-RS.
 * <p>
 * Files live on the classpath under {@code /webdemo/} (packaged from
 * {@code src/main/resources/webdemo/}). Opening the HTML directly from
 * disk still works too; see {@link TusCorsFilter}.
 */
@Path("/tus/demo")
public class TusDemoPageResource {

    @GET
    @PermitAll
    @Produces(MediaType.TEXT_HTML)
    public Response page(@Context UriInfo uriInfo) {
        // Force a trailing slash so the HTML's relative <link href="tus-upload-demo.css">
        // resolves to /api/tus/demo/tus-upload-demo.css and not /api/tus/tus-upload-demo.css.
        String path = uriInfo.getRequestUri().getPath();
        if (!path.endsWith("/")) {
            URI withSlash = uriInfo.getRequestUriBuilder().replacePath(path + "/").build();
            return Response.status(Response.Status.MOVED_PERMANENTLY).location(withSlash).build();
        }
        return stream("/webdemo/tus-upload-demo.html", MediaType.TEXT_HTML);
    }

    @GET
    @Path("/tus-upload-demo.css")
    @PermitAll
    @Produces("text/css")
    public Response css() {
        return stream("/webdemo/tus-upload-demo.css", "text/css");
    }

    private static Response stream(String classpathResource, String mediaType) {
        InputStream in = TusDemoPageResource.class.getResourceAsStream(classpathResource);
        if (in == null) throw new NotFoundException("Not packaged: " + classpathResource);
        return Response.ok(in, mediaType).build();
    }
}