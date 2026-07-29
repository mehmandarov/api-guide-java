package com.mehmandarov.confapi.gatekeepers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * <strong>The Gatekeeper – Body Sanitization Interceptor</strong>
 * <p>
 * A {@code ReaderInterceptor} that sanitizes the JSON request body by
 * stripping HTML tags and {@code javascript:} protocol strings before
 * the body is deserialized into a Java object.
 * <p>
 * Using {@code ReaderInterceptor} (instead of reading the entity stream
 * in a {@code ContainerRequestFilter}) is the correct JAX-RS approach:
 * <ul>
 *   <li>It runs at deserialization time, not at filter time</li>
 *   <li>It works with both blocking and non-blocking runtimes (Quarkus Reactive, etc.)</li>
 *   <li>It has access to the entity stream in a safe, read-once manner</li>
 * </ul>
 *
 * @see InputSanitizationFilter for query parameter sanitization
 */
@Provider
public class InputSanitizationInterceptor implements ReaderInterceptor {

    private static final Logger LOG = Logger.getLogger(InputSanitizationInterceptor.class.getName());

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext ctx)
            throws IOException, WebApplicationException {

        // Only sanitize textual/JSON payloads. Binary bodies (multipart
        // uploads, application/octet-stream, images, PDFs, …) must pass
        // through untouched – decoding them as UTF-8 text and re-encoding
        // corrupts the bytes (and the multipart boundary structure),
        // which would otherwise surface as a 500 at deserialization time.
        InputStream original = ctx.getInputStream();
        if (original != null && shouldSanitize(ctx.getMediaType())) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
            original.transferTo(buf);
            byte[] rawBytes = buf.toByteArray();

            if (rawBytes.length > 0) {
                String body = new String(rawBytes, StandardCharsets.UTF_8);
                String sanitized = InputSanitizationFilter.sanitize(body);

                if (!body.equals(sanitized)) {
                    LOG.fine("[SANITIZER] Cleaned request body");
                }

                ctx.setInputStream(new ByteArrayInputStream(
                        sanitized.getBytes(StandardCharsets.UTF_8)));
            }
        }

        return ctx.proceed();
    }

    /**
     * Sanitization only makes sense for text-based payloads. Skip anything
     * that isn't JSON or {@code text/*}. A {@code null} media type is treated
     * as sanitizable so the interceptor stays testable in isolation.
     */
    private static boolean shouldSanitize(MediaType mediaType) {
        if (mediaType == null) {
            return true;
        }
        return "text".equalsIgnoreCase(mediaType.getType())
                || mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }
}

