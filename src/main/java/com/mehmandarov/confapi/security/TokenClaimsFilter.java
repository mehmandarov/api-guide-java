package com.mehmandarov.confapi.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * <strong>The Security Shield — Token Claims Filter</strong>
 * <p>
 * Extracts custom JWT claims and propagates them as JAX-RS request
 * properties, so downstream resources can use them without coupling
 * to the {@code JsonWebToken} API directly.
 * <p>
 * Example claims extracted:
 * <ul>
 *   <li>{@code speaker_id} — the speaker profile linked to this user</li>
 *   <li>{@code tenant} — multi-tenancy identifier</li>
 * </ul>
 */
@Provider
public class TokenClaimsFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TokenClaimsFilter.class.getName());

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        try {
            if (jwt != null && jwt.getRawToken() != null) {
                // Propagate standard claims
                ctx.setProperty("jwt.subject", jwt.getSubject());
                ctx.setProperty("jwt.issuer", jwt.getIssuer());
                ctx.setProperty("jwt.groups", jwt.getGroups());

                // Propagate custom claims if present
                propagateClaim(ctx, "speaker_id");
                propagateClaim(ctx, "tenant");

                LOG.fine(() -> String.format(
                        "[SECURITY] JWT sub=%s iss=%s groups=%s",
                        jwt.getSubject(), jwt.getIssuer(), jwt.getGroups()));
            }
        } catch (IllegalStateException e) {
            // Not a real JWT (e.g. test security principal) — skip claim extraction
            LOG.fine(() -> "[SECURITY] Non-JWT principal, skipping claim extraction");
        }
    }

    private void propagateClaim(ContainerRequestContext ctx, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value != null) {
            ctx.setProperty("jwt." + claimName, value);
        }
    }
}
