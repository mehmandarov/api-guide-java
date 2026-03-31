package com.mehmandarov.confapi.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mehmandarov.confapi.error.ProblemDetail;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <strong>The Security Shield — Request Signature Verification Filter</strong>
 * <p>
 * Verifies that incoming request payloads have not been tampered with
 * by checking an HMAC-SHA256 signature sent in the {@code X-Signature} header.
 * <p>
 * This is a common pattern for webhook receivers and high-security APIs
 * where you need to guarantee <em>payload integrity</em> beyond what
 * TLS provides (e.g., verifying the sender has the shared secret).
 * <p>
 * <strong>How it works:</strong>
 * <ol>
 *   <li>Client computes {@code HMAC-SHA256(body, sharedSecret)}</li>
 *   <li>Client sends the hex-encoded result in {@code X-Signature} header</li>
 *   <li>This filter recomputes the HMAC and compares</li>
 *   <li>If they don't match → 401 with Problem Details</li>
 * </ol>
 * <p>
 * Apply {@link SignatureRequired @SignatureRequired} to opt-in per resource/method.
 * The shared secret is configured via MicroProfile Config:
 * {@code api.signature.secret}.
 */
@Provider
@SignatureRequired
public class RequestSignatureFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(RequestSignatureFilter.class.getName());
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Inject
    @ConfigProperty(name = "api.signature.secret", defaultValue = "change-me-in-production")
    String sharedSecret;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String signature = ctx.getHeaderString(SIGNATURE_HEADER);

        if (signature == null || signature.isBlank()) {
            abort(ctx, "Missing " + SIGNATURE_HEADER + " header");
            return;
        }

        // Read the body (and restore the stream for downstream consumption)
        byte[] body = readBody(ctx);

        try {
            String expected = computeHmac(body);

            if (!secureEquals(expected, signature)) {
                LOG.warning(() -> "[SECURITY] Signature mismatch on " + ctx.getUriInfo().getPath());
                abort(ctx, "Invalid request signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.log(Level.SEVERE, "HMAC computation failed", e);
            abort(ctx, "Signature verification unavailable");
        }
    }

    private byte[] readBody(ContainerRequestContext ctx) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        ctx.getEntityStream().transferTo(buf);
        byte[] body = buf.toByteArray();
        // Restore the entity stream so JAX-RS can still deserialize the body
        ctx.setEntityStream(new ByteArrayInputStream(body));
        return body;
    }

    public String computeHmac(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(data);
        return HexFormat.of().formatHex(hash);
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private boolean secureEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private void abort(ContainerRequestContext ctx, String detail) {
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.UNAUTHORIZED.getStatusCode(),
                "Signature Verification Failed",
                detail
        ).withType("urn:problem-type:invalid-signature");

        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build());
    }
}

