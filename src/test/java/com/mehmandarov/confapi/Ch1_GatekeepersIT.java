package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import com.mehmandarov.confapi.support.TestTokens;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Pattern 1: The Gatekeepers — End-to-End</strong>
 * <p>
 * Proves the full HTTP pipeline: sanitization filter → validation →
 * clean business logic → correct response. Your endpoint code never
 * touches dirty input.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Ch1 IT — The Gatekeepers")
class Ch1_GatekeepersIT {

    @Nested
    @DisplayName("Input Sanitization — dangerous payloads don't crash the server")
    class Sanitization {

        @Test
        @DisplayName("POST with <script> tags in body is handled cleanly (no 500)")
        void xssInBodyDoesNotCrash() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                      "title": "Good Title <script>alert('xss')</script>",
                      "abstract": "A <b>bold</b> abstract.",
                      "level": "INTERMEDIATE",
                      "speakerId": "spk-duke",
                      "startTime": "2026-10-16T11:00:00",
                      "durationMinutes": 50
                    }
                    """)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(anyOf(is(201), is(401)));
            // 401 without JWT is fine — the point is it's NOT a 500.
        }
    }

    @Nested
    @DisplayName("Bean Validation — invalid input is rejected before business logic runs")
    class Validation {

        @Test
        @DisplayName("Empty title → 400 with violations array")
        void emptyTitleRejected() {
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
                .body("type", is("urn:problem-type:validation-error"))
                .body("extensions.violations", not(empty()));
        }

        @Test
        @DisplayName("@NoProfanity custom validator rejects 'phishing' in title")
        void noProfanityRejectsBlockedWords() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forOrganizer())
                .body("""
                    {
                      "title": "Learn about phishing techniques",
                      "abstract": "Phishing abstract.",
                      "level": "BEGINNER",
                      "speakerId": "spk-duke",
                      "startTime": "2026-10-16T11:00:00",
                      "durationMinutes": 50
                    }
                    """)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(400)
                .body("type", is("urn:problem-type:validation-error"))
                .body("extensions.violations.find { it.field == 'title' }.message",
                      containsString("phishing"));
        }
    }

    @Nested
    @DisplayName("Read Endpoints — public data flows through the clean pipeline")
    class PublicReads {

        @Test
        @DisplayName("GET /api/v1/sessions returns seeded sessions")
        void listSessions() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(3))
                .body("[0].title", notNullValue())
                .body("[0].id", notNullValue());
        }

        @Test
        @DisplayName("GET /api/v1/speakers returns seeded speakers")
        void listSpeakers() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/speakers")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(2))
                .body("find { it.id == 'spk-duke' }.name", is("Duke Java"));
        }

        @Test
        @DisplayName("GET /api/v1/rooms returns seeded rooms")
        void listRooms() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/rooms")
            .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(3));
        }
    }
}
