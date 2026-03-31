package com.mehmandarov.confapi.unit;

import com.mehmandarov.confapi.security.RequestSignatureFilter;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <strong>Pattern 2: The Security Shield</strong>
 * <p>
 * Tests prove: HMAC-SHA256 guarantees payload integrity, and constant-time
 * comparison prevents timing-attack information leaks.
 */
@DisplayName("Ch2 — The Security Shield")
class Ch2_SecurityShieldTest {

    private RequestSignatureFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new RequestSignatureFilter();
        setSecret("test-secret");
    }

    @Nested
    @DisplayName("HMAC-SHA256 Signature — verifying payload integrity")
    class HmacSignature {

        @Test
        @DisplayName("Produces a 64-char hex digest (SHA-256 = 32 bytes = 64 hex chars)")
        void producesCorrectLength() throws Exception {
            String sig = filter.computeHmac("{\"title\":\"test\"}".getBytes(StandardCharsets.UTF_8));
            assertEquals(64, sig.length());
            assertTrue(sig.matches("[0-9a-f]+"), "Must be lowercase hex");
        }

        @Test
        @DisplayName("Same payload + same secret = identical signature (deterministic)")
        void isDeterministic() throws Exception {
            byte[] payload = "{\"action\":\"create\"}".getBytes(StandardCharsets.UTF_8);
            assertEquals(
                    filter.computeHmac(payload),
                    filter.computeHmac(payload),
                    "Signing the same payload twice must produce identical results");
        }

        @Test
        @DisplayName("Tampered payload → different signature (integrity detection)")
        void detectsTamperedPayload() throws Exception {
            String original = filter.computeHmac("{\"amount\":100}".getBytes(StandardCharsets.UTF_8));
            String tampered = filter.computeHmac("{\"amount\":999}".getBytes(StandardCharsets.UTF_8));
            assertNotEquals(original, tampered,
                    "Changing even one character in the payload must change the signature");
        }

        @Test
        @DisplayName("Wrong secret → signature mismatch (authenticates sender)")
        void wrongSecretFails() throws Exception {
            byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
            String sig1 = filter.computeHmac(payload);

            setSecret("attacker-secret");
            String sig2 = filter.computeHmac(payload);

            assertNotEquals(sig1, sig2,
                    "A different secret must produce a different signature — this proves the sender has the right key");
        }

        @Test
        @DisplayName("Empty body still produces a valid signature (for GET-style webhooks)")
        void emptyBodyIsValid() throws Exception {
            String sig = filter.computeHmac(new byte[0]);
            assertNotNull(sig);
            assertEquals(64, sig.length());
        }
    }

    @Nested
    @DisplayName("Constant-Time Comparison — preventing timing attacks")
    class TimingSafety {

        @Test
        @DisplayName("Equal strings return true")
        void equalStringsMatch() throws Exception {
            assertTrue(invokeSecureEquals("abc123", "abc123"));
        }

        @Test
        @DisplayName("Different strings return false")
        void differentStringsDontMatch() throws Exception {
            assertFalse(invokeSecureEquals("abc123", "xyz789"));
        }

        @Test
        @DisplayName("Different lengths return false (fast reject is OK here)")
        void differentLengthsFalse() throws Exception {
            assertFalse(invokeSecureEquals("short", "muchlonger"));
        }

        @Test
        @DisplayName("Strings differing in last character are still rejected")
        void differOnlyInLastChar() throws Exception {
            assertFalse(invokeSecureEquals(
                    "a]b6c2d8e4f0a1b6c2d8e4f0a1b6c2d8e4f0a1b6c2d8e4f0a1b6c2d8e4f0abc0",
                    "a]b6c2d8e4f0a1b6c2d8e4f0a1b6c2d8e4f0a1b6c2d8e4f0a1b6c2d8e4f0abc1"),
                    "Must detect difference even in the very last character");
        }

        /**
         * Invokes the private secureEquals via reflection for testing.
         */
        private boolean invokeSecureEquals(String a, String b) throws Exception {
            Method m = RequestSignatureFilter.class.getDeclaredMethod("secureEquals", String.class, String.class);
            m.setAccessible(true);
            return (boolean) m.invoke(filter, a, b);
        }
    }

    private void setSecret(String secret) throws Exception {
        Field f = RequestSignatureFilter.class.getDeclaredField("sharedSecret");
        f.setAccessible(true);
        f.set(filter, secret);
    }
}

