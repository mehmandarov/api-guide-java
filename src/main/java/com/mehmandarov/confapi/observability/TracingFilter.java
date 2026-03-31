package com.mehmandarov.confapi.observability;

import io.opentelemetry.api.trace.Span;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * <strong>The Lens — Tracing Filter</strong>
 * <p>
 * Enriches the current OpenTelemetry span with domain-specific attributes
 * so you can search traces by session ID, API version, or caller.
 * <p>
 * This is purely additive — if no OTel agent is attached, the
 * {@link Span#current()} call returns a no-op span and the filter
 * has zero overhead.
 */
@Provider
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        Span span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("http.route", req.getUriInfo().getPath());

            // Tag with API version extracted from the path
            String path = req.getUriInfo().getPath();
            if (path.startsWith("v2/")) {
                span.setAttribute("api.version", "v2");
            } else {
                span.setAttribute("api.version", "v1");
            }

            // If there's a path param "id", tag the span with it
            var pathParams = req.getUriInfo().getPathParameters();
            if (pathParams.containsKey("id")) {
                span.setAttribute("resource.id", pathParams.getFirst("id"));
            }

            // Tag with correlation ID from CorrelationIdFilter
            Object correlationId = req.getProperty(CorrelationIdFilter.PROPERTY_NAME);
            if (correlationId != null) {
                span.setAttribute("request.correlation_id", correlationId.toString());
            }

            // Tag with caller identity if available
            if (req.getSecurityContext() != null
                    && req.getSecurityContext().getUserPrincipal() != null) {
                span.setAttribute("enduser.id",
                        req.getSecurityContext().getUserPrincipal().getName());
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext req,
                       ContainerResponseContext resp) throws IOException {
        Span span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("http.response.status_code", resp.getStatus());
        }
    }
}

