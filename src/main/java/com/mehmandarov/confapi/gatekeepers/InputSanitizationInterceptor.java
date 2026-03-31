package com.mehmandarov.confapi.gatekeepers;

import jakarta.ws.rs.WebApplicationException;
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
 * <strong>The Gatekeeper — Body Sanitization Interceptor</strong>
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

        InputStream original = ctx.getInputStream();
        if (original != null) {
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
}

