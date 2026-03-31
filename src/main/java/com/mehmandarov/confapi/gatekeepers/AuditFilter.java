package com.mehmandarov.confapi.gatekeepers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * <strong>The Gatekeeper — Audit Filter</strong>
 * <p>
 * A name-bound filter pair (request + response) that logs:
 * <ul>
 *   <li>HTTP method &amp; URI</li>
 *   <li>Caller principal (from JWT / security context)</li>
 *   <li>Response status code</li>
 *   <li>Request processing time</li>
 * </ul>
 * <p>
 * Apply {@link Audited @Audited} to any resource class or method to enable.
 */
@Provider
@Audited
public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(AuditFilter.class.getName());
    private static final String START_TIME = "com.mehmandarov.confapi.audit.startTime";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        request.setProperty(START_TIME, System.nanoTime());

        String caller = principalName(request.getSecurityContext());
        LOG.info(() -> String.format("[AUDIT] >>> %s %s | caller=%s | at=%s",
                request.getMethod(),
                request.getUriInfo().getRequestUri(),
                caller,
                Instant.now()));
    }

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response) throws IOException {
        Object startProp = request.getProperty(START_TIME);
        if (startProp == null) {
            return; // Request filter didn't run (e.g. security short-circuit)
        }
        long startNanos = (long) startProp;
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        String caller = principalName(request.getSecurityContext());
        LOG.info(() -> String.format("[AUDIT] <<< %s %s | status=%d | caller=%s | elapsed=%dms",
                request.getMethod(),
                request.getUriInfo().getRequestUri(),
                response.getStatus(),
                caller,
                elapsedMs));
    }

    private String principalName(SecurityContext ctx) {
        if (ctx != null && ctx.getUserPrincipal() != null) {
            return ctx.getUserPrincipal().getName();
        }
        return "anonymous";
    }
}

