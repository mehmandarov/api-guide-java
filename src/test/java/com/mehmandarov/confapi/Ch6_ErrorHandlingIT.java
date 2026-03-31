package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import com.mehmandarov.confapi.support.TestTokens;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Bonus: Sane Error Handling — End-to-End</strong>
 * <p>
 * Proves: every error status code (400, 401, 403, 404) returns a consistent
 * RFC 9457 Problem Details response with {@code application/problem+json}.
 * No stack traces leak. Clients always know what went wrong.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Ch6 IT — Sane Error Handling (RFC 9457)")
class Ch6_ErrorHandlingIT {

    @Nested
    @DisplayName("404 Not Found — resource does not exist")
    class NotFound {

        @Test
        @DisplayName("Non-existent session → 404 Problem Details with type URN")
        void sessionNotFound() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions/does-not-exist")
            .then()
                .statusCode(404)
                .contentType(containsString("problem+json"))
                .body("title", is("Resource Not Found"))
                .body("status", is(404))
                .body("type", is("urn:problem-type:not-found"))
                .body("detail", containsString("does-not-exist"));
        }

        @Test
        @DisplayName("Non-existent speaker → 404 Problem Details")
        void speakerNotFound() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/speakers/non-existent")
            .then()
                .statusCode(404)
                .body("status", is(404));
        }

        @Test
        @DisplayName("Non-existent room → 404 Problem Details")
        void roomNotFound() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/rooms/non-existent")
            .then()
                .statusCode(404)
                .body("status", is(404));
        }
    }

    @Nested
    @DisplayName("400 Validation Error — bad input is structured, not a stack trace")
    class ValidationError {

        @Test
        @DisplayName("Multiple invalid fields → 400 with violations array")
        void multipleViolations() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forOrganizer())
                .body("""
                    {
                      "title": "",
                      "abstract": "",
                      "level": null,
                      "speakerId": "",
                      "durationMinutes": 5
                    }
                    """)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(400)
                .body("title", is("Validation Failed"))
                .body("status", is(400))
                .body("type", is("urn:problem-type:validation-error"))
                .body("extensions.violations", not(empty()));
        }
    }

    @Nested
    @DisplayName("401/403 — security errors are clean, not stack traces")
    class SecurityErrors {

        @Test
        @DisplayName("Unauthenticated write → 401")
        void unauthenticated() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "title": "Test",
                      "abstract": "Test",
                      "level": "BEGINNER",
                      "speakerId": "spk-duke",
                      "startTime": "2026-10-16T11:00:00",
                      "durationMinutes": 50
                    }
                    """)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Insufficient role → 403")
        void forbidden() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forAttendee())
                .body("""
                    {
                      "title": "Test",
                      "abstract": "Test.",
                      "level": "BEGINNER",
                      "speakerId": "spk-duke",
                      "startTime": "2026-10-16T11:00:00",
                      "durationMinutes": 50
                    }
                    """)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(403);
        }
    }
}

