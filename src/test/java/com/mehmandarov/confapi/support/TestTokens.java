package com.mehmandarov.confapi.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Generates real RS256 JWT tokens for integration tests.
 * <p>
 * These tokens are signed with the test private key
 * ({@code src/test/resources/test-private-key.pem}) that matches the
 * public key baked into the application ({@code META-INF/publicKey.pem}).
 * The container validates them through the standard MicroProfile JWT pipeline —
 * no mocks, no fakes, no runtime-specific shortcuts.
 * <p>
 * Claims match the structure from {@code generate-jwt.sh}:
 * {@code iss}, {@code sub}, {@code upn}, {@code groups}, {@code speaker_id}.
 */
public final class TestTokens {

    private static final String ISSUER = "https://confapi.example.com";
    private static final RSAPrivateKey PRIVATE_KEY;

    static {
        try {
            PRIVATE_KEY = loadPrivateKey();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TestTokens() { }

    /** Token with {@code groups: ["ORGANIZER"]} — full CRUD access. */
    public static String forOrganizer() {
        return generate("organizer1", "ORGANIZER");
    }

    /** Token with {@code groups: ["SPEAKER"]} — read + own sessions. */
    public static String forSpeaker() {
        return generate("speaker1", "SPEAKER");
    }

    /** Token with {@code groups: ["ATTENDEE"]} — read-only. */
    public static String forAttendee() {
        return generate("attendee1", "ATTENDEE");
    }

    private static String generate(String user, String role) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .subject(user)
                    .claim("upn", user + "@example.com")
                    .claim("groups", List.of(role))
                    .claim("speaker_id", "spk-duke")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(PRIVATE_KEY));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test JWT for " + user, e);
        }
    }

    private static RSAPrivateKey loadPrivateKey() throws Exception {
        try (InputStream is = TestTokens.class.getResourceAsStream("/test-private-key.pem")) {
            if (is == null) {
                throw new IllegalStateException(
                        "test-private-key.pem not found on classpath. "
                                + "Run: cp /tmp/confapi_private.pem src/test/resources/test-private-key.pem");
            }
            String pem = new String(is.readAllBytes());
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        }
    }
}

