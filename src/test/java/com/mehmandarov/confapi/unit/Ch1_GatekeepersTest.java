package com.mehmandarov.confapi.unit;

import com.mehmandarov.confapi.gatekeepers.InputSanitizationFilter;
import com.mehmandarov.confapi.gatekeepers.InputSanitizationInterceptor;
import com.mehmandarov.confapi.gatekeepers.NoProfanityValidator;
import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <strong>Pattern 1: The Gatekeepers</strong>
 * <p>
 * These tests prove the core thesis: <em>business logic should never see dirty input.</em>
 * We test the sanitization logic, the interceptor pattern, and custom validators.
 */
@DisplayName("Ch1 — The Gatekeepers")
class Ch1_GatekeepersTest {

    @Nested
    @DisplayName("Input Sanitization — strips dangerous content before business logic sees it")
    class Sanitization {

        @Test
        @DisplayName("XSS via <script> tags is stripped transparently")
        void stripsScriptTags() {
            String dirty = "My Talk <script>alert('xss')</script>";
            String clean = InputSanitizationFilter.sanitize(dirty);
            assertEquals("My Talk alert('xss')", clean);
            assertFalse(clean.contains("<"), "No HTML tags should survive");
        }

        @Test
        @DisplayName("javascript: protocol injection is neutralized")
        void stripsJavascriptProtocol() {
            assertEquals("void(0)", InputSanitizationFilter.sanitize("javascript:void(0)"));
        }

        @Test
        @DisplayName("Clean input passes through unchanged — no false positives")
        void cleanInputUnchanged() {
            String title = "Building APIs with Jakarta EE 11 & MicroProfile 7";
            assertEquals(title, InputSanitizationFilter.sanitize(title));
        }

        @Test
        @DisplayName("Null input is handled safely — no NPE")
        void nullSafe() {
            assertNull(InputSanitizationFilter.sanitize(null));
        }

        @ParameterizedTest(name = "OWASP payload: {0}")
        @DisplayName("Handles OWASP Top 10 XSS payloads")
        @CsvSource({
            "'<img src=x onerror=alert(1)>', ''",
            "'<a href=\"javascript:alert(1)\">click</a>', 'click'",
            "'<svg onload=alert(1)>', ''",
            "'normal text', 'normal text'"
        })
        void handlesOwaspPayloads(String input, String expected) {
            assertEquals(expected, InputSanitizationFilter.sanitize(input));
        }
    }

    @Nested
    @DisplayName("ReaderInterceptor — transforms body BEFORE deserialization (runtime-safe)")
    class BodyInterceptor {

        @Test
        @DisplayName("Interceptor sanitizes request body and passes cleaned version downstream")
        void interceptorSanitizesBody() throws Exception {
            String dirtyBody = "{\"title\":\"<script>alert('xss')</script> Real Title\"}";
            String expectedClean = "{\"title\":\"alert('xss') Real Title\"}";

            InputSanitizationInterceptor interceptor = new InputSanitizationInterceptor();

            // Track what proceed() receives
            AtomicReference<String> streamAfterSanitization = new AtomicReference<>();

            ReaderInterceptorContext mockCtx = createMockReaderContext(dirtyBody, streamAfterSanitization);
            interceptor.aroundReadFrom(mockCtx);

            assertEquals(expectedClean, streamAfterSanitization.get(),
                    "The body stream passed to proceed() should be sanitized");
        }

        @Test
        @DisplayName("Clean body passes through without modification")
        void cleanBodyPassesThrough() throws Exception {
            String cleanBody = "{\"title\":\"Perfectly Normal Title\"}";
            AtomicReference<String> streamAfterSanitization = new AtomicReference<>();

            InputSanitizationInterceptor interceptor = new InputSanitizationInterceptor();
            ReaderInterceptorContext mockCtx = createMockReaderContext(cleanBody, streamAfterSanitization);
            interceptor.aroundReadFrom(mockCtx);

            assertEquals(cleanBody, streamAfterSanitization.get());
        }

        /**
         * Creates a minimal ReaderInterceptorContext that captures the input stream
         * at the point proceed() is called — proving the interceptor modified it.
         */
        private ReaderInterceptorContext createMockReaderContext(
                String body, AtomicReference<String> capturedStream) {

            return new ReaderInterceptorContext() {
                private InputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

                @Override public InputStream getInputStream() { return is; }
                @Override public void setInputStream(InputStream is) { this.is = is; }

                @Override
                public Object proceed() throws IOException, WebApplicationException {
                    // Capture what the interceptor passed downstream
                    byte[] bytes = is.readAllBytes();
                    capturedStream.set(new String(bytes, StandardCharsets.UTF_8));
                    return null;
                }

                // --- Unused stubs ---
                @Override public Object getProperty(String name) { return null; }
                @Override public java.util.Collection<String> getPropertyNames() { return java.util.List.of(); }
                @Override public void setProperty(String name, Object object) { }
                @Override public void removeProperty(String name) { }
                @Override public Annotation[] getAnnotations() { return new Annotation[0]; }
                @Override public void setAnnotations(Annotation[] annotations) { }
                @Override public Class<?> getType() { return null; }
                @Override public void setType(Class<?> type) { }
                @Override public Type getGenericType() { return null; }
                @Override public void setGenericType(Type genericType) { }
                @Override public MediaType getMediaType() { return null; }
                @Override public void setMediaType(MediaType mediaType) { }
                @Override public MultivaluedMap<String, String> getHeaders() { return null; }
            };
        }
    }

    @Nested
    @DisplayName("Custom Validators — domain-specific constraints via @NoProfanity")
    class CustomValidation {

        private NoProfanityValidator validator;
        private ConstraintValidatorContext ctx;

        @BeforeEach
        void setUp() {
            validator = new NoProfanityValidator();
            ctx = new StubConstraintValidatorContext();
        }

        @ParameterizedTest(name = "rejects: \"{0}\"")
        @DisplayName("Blocked words are rejected regardless of case")
        @ValueSource(strings = {
                "Learn about phishing attacks",
                "This is spam content",
                "Avoid scam websites",
                "PHISHING in uppercase"
        })
        void rejectsBlockedWords(String input) {
            assertFalse(validator.isValid(input, ctx),
                    "Should reject '" + input + "' as it contains a blocked word");
        }

        @Test
        @DisplayName("Clean session titles pass validation")
        void allowsCleanTitles() {
            assertTrue(validator.isValid("An Opinionated Guide to Bulletproof APIs", ctx));
        }

        @Test
        @DisplayName("Null and blank are allowed — @NotBlank handles those separately")
        void nullAndBlankDelegatedToOtherConstraints() {
            assertTrue(validator.isValid(null, ctx), "null should pass — @NotBlank's job");
            assertTrue(validator.isValid("  ", ctx), "blank should pass — @NotBlank's job");
        }

        @Test
        @DisplayName("Partial matches don't trigger false positives")
        void noFalsePositivesOnPartialMatches() {
            assertTrue(validator.isValid("Visit the spa for relaxation", ctx),
                    "'spa' should NOT trigger 'spam' blocking");
        }
    }

    // ── Stub for ConstraintValidatorContext ──────────────────────

    static class StubConstraintValidatorContext implements ConstraintValidatorContext {
        @Override public void disableDefaultConstraintViolation() { }
        @Override public String getDefaultConstraintMessageTemplate() { return ""; }
        @Override public ClockProvider getClockProvider() { return null; }
        @Override public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String tpl) {
            return new ConstraintViolationBuilder() {
                @Override public NodeBuilderDefinedContext addNode(String n) { return null; }
                @Override public NodeBuilderCustomizableContext addPropertyNode(String n) { return null; }
                @Override public LeafNodeBuilderCustomizableContext addBeanNode() { return null; }
                @Override public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String n, Class<?> c, Integer i) { return null; }
                @Override public NodeBuilderDefinedContext addParameterNode(int i) { return null; }
                @Override public ConstraintValidatorContext addConstraintViolation() { return StubConstraintValidatorContext.this; }
            };
        }
        @Override public <T> T unwrap(Class<T> type) { return null; }
    }
}

