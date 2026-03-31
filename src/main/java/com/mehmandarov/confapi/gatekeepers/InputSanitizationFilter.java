package com.mehmandarov.confapi.gatekeepers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * <strong>The Gatekeeper — Input Sanitization Filter (Query Params)</strong>
 * <p>
 * A {@code @PreMatching} request filter that strips potentially dangerous
 * content (HTML tags, script injections) from all query parameters.
 * <p>
 * Body sanitization is handled separately by
 * {@link InputSanitizationInterceptor} (a {@code ReaderInterceptor}),
 * which is the correct JAX-RS API for transforming request bodies and
 * works correctly with both blocking and non-blocking runtimes.
 * <p>
 * By running <em>before</em> resource matching, no business code ever
 * sees unsanitized query input.
 */
@Provider
@PreMatching
public class InputSanitizationFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(InputSanitizationFilter.class.getName());

    /** Regex to strip HTML/XML tags. */
    static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    /** Regex to strip javascript: URIs. */
    static final Pattern JS_PROTOCOL = Pattern.compile("(?i)javascript\\s*:");

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        var uriBuilder = ctx.getUriInfo().getRequestUriBuilder();
        boolean queryChanged = false;
        for (var entry : ctx.getUriInfo().getQueryParameters().entrySet()) {
            for (String value : entry.getValue()) {
                String sanitized = sanitize(value);
                if (!sanitized.equals(value)) {
                    uriBuilder.replaceQueryParam(entry.getKey(), sanitized);
                    queryChanged = true;
                    LOG.fine(() -> "Sanitized query param [" + entry.getKey() + "]");
                }
            }
        }
        if (queryChanged) {
            ctx.setRequestUri(ctx.getUriInfo().getBaseUri(), uriBuilder.build());
        }
    }

    /**
     * Strip HTML tags and javascript: protocol strings.
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        String result = HTML_TAG.matcher(input).replaceAll("");
        result = JS_PROTOCOL.matcher(result).replaceAll("");
        return result;
    }
}
