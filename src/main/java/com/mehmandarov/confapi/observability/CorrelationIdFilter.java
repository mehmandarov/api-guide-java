package com.mehmandarov.confapi.observability;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

/**
 * <strong>The Lens — Correlation ID Filter</strong>
 * <p>
 * Ensures every request/response pair carries a unique correlation ID
 * for end-to-end request tracing across services and log aggregation.
 * <p>
 * Behavior:
 * <ul>
 *   <li>If the client sends {@code X-Request-Id}, it is preserved.</li>
 *   <li>Otherwise, a new UUID is generated.</li>
 *   <li>The ID is stored as a request property (available to resources
 *       and other filters) and echoed back as a response header.</li>
 * </ul>
 */
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String PROPERTY_NAME = "correlation.id";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String correlationId = request.getHeaderString(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        request.setProperty(PROPERTY_NAME, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response) throws IOException {
        Object correlationId = request.getProperty(PROPERTY_NAME);
        if (correlationId != null) {
            response.getHeaders().putSingle(HEADER_NAME, correlationId.toString());
        }
    }
}

