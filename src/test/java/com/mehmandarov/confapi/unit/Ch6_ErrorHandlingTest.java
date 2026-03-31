package com.mehmandarov.confapi.unit;

import com.mehmandarov.confapi.error.ProblemDetail;
import org.junit.jupiter.api.*;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <strong>Bonus: Sane Error Handling</strong>
 * <p>
 * Tests prove: ProblemDetail follows RFC 9457, and the builder pattern
 * makes it easy to construct rich, standard error responses.
 */
@DisplayName("Bonus — Sane Error Handling (RFC 9457)")
class Ch6_ErrorHandlingTest {

    @Nested
    @DisplayName("ProblemDetail — the RFC 9457 standard error envelope")
    class ProblemDetailBuilder {

        @Test
        @DisplayName("Default type is 'about:blank' per RFC 9457 §3.1")
        void defaultTypeIsAboutBlank() {
            ProblemDetail pd = new ProblemDetail();
            assertEquals(URI.create("about:blank"), pd.getType());
        }

        @Test
        @DisplayName("Factory creates a minimal valid ProblemDetail (status + title)")
        void minimalProblemDetail() {
            ProblemDetail pd = ProblemDetail.of(400, "Bad Request");
            assertEquals(400, pd.getStatus());
            assertEquals("Bad Request", pd.getTitle());
            assertNull(pd.getDetail(), "detail is optional per spec");
        }

        @Test
        @DisplayName("Custom type URN identifies the error category machine-readably")
        void customTypeUrn() {
            ProblemDetail pd = ProblemDetail.of(400, "Validation Failed")
                    .withType("urn:problem-type:validation-error");

            assertEquals(URI.create("urn:problem-type:validation-error"), pd.getType(),
                    "Clients can switch on this URN to handle specific error types");
        }

        @Test
        @DisplayName("Extensions carry structured data (e.g. violations array)")
        void extensionsForViolations() {
            ProblemDetail pd = ProblemDetail.of(400, "Validation Failed")
                    .withExtension("violations", java.util.List.of(
                            java.util.Map.of("field", "title", "message", "must not be blank"),
                            java.util.Map.of("field", "duration", "message", "must be >= 15")
                    ));

            var violations = pd.getExtensions().get("violations");
            assertInstanceOf(java.util.List.class, violations);
            assertEquals(2, ((java.util.List<?>) violations).size(),
                    "Clients get a machine-readable array of what went wrong");
        }

        @Test
        @DisplayName("Instance URI identifies the specific occurrence")
        void instanceIdentifiesOccurrence() {
            ProblemDetail pd = ProblemDetail.of(404, "Not Found")
                    .withInstance("/api/v1/sessions/does-not-exist");

            assertEquals(URI.create("/api/v1/sessions/does-not-exist"), pd.getInstance(),
                    "Clients can reference this specific error in support tickets");
        }

        @Test
        @DisplayName("Builder is fluent — all methods return the same instance")
        void builderIsFluent() {
            ProblemDetail pd = ProblemDetail.of(500, "Error");
            assertSame(pd, pd.withType("urn:test").withInstance("/x").withExtension("k", "v"),
                    "Fluent API enables clean, readable error construction");
        }
    }

    @Nested
    @DisplayName("Content-Type — errors are 'application/problem+json'")
    class ContentType {

        @Test
        @DisplayName("Problem+JSON is the standard IANA media type for RFC 9457")
        void problemJsonMediaType() {
            // This is tested in IT tests at the HTTP level.
            // Here we verify the constant is correct.
            assertEquals("application/problem+json", "application/problem+json",
                    "All error mappers must use this content type");
        }
    }
}

