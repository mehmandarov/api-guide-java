package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import com.mehmandarov.confapi.support.TestTokens;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Pattern 2: The Security Shield — End-to-End</strong>
 * <p>
 * Proves the full RBAC chain: no token → 401, wrong role → 403,
 * right role → success. Uses real RS256 JWTs validated by the container's
 * MicroProfile JWT pipeline — no mocks, no fakes.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Ch2 IT — The Security Shield")
class Ch2_SecurityShieldIT {

    private static final String SESSION_BODY = """
            {
              "title": "Secure Session",
              "abstract": "Testing RBAC with Jakarta Security.",
              "level": "ADVANCED",
              "track": "Security",
              "speakerId": "spk-duke",
              "startTime": "2026-10-16T14:00:00",
              "durationMinutes": 50
            }
            """;

    @Nested
    @DisplayName("Unauthenticated — no token means no access to write operations")
    class Unauthenticated {

        @Test
        @DisplayName("POST without a token → 401 Unauthorized")
        void postWithoutToken() {
            given()
                .contentType(ContentType.JSON)
                .body(SESSION_BODY)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("GET is public — no token needed → 200")
        void getIsPublic() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions")
            .then()
                .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Wrong Role — authenticated but insufficient privileges")
    class WrongRole {

        @Test
        @DisplayName("ATTENDEE cannot create sessions → 403 Forbidden")
        void attendeeCannotCreate() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forAttendee())
                .body(SESSION_BODY)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("SPEAKER cannot delete sessions → 403 Forbidden")
        void speakerCannotDelete() {
            String sessionId = given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions")
            .then()
                .extract().jsonPath().getString("[0].id");

            given()
                .header("Authorization", "Bearer " + TestTokens.forSpeaker())
            .when()
                .delete("/api/v1/sessions/" + sessionId)
            .then()
                .statusCode(403);
        }
    }

    @Nested
    @DisplayName("Correct Role — ORGANIZER has full CRUD access")
    class CorrectRole {

        @Test
        @DisplayName("ORGANIZER creates a session → 201 with Location header")
        void organizerCreates() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forOrganizer())
                .body(SESSION_BODY)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("title", is("Secure Session"));
        }

        @Test
        @DisplayName("ORGANIZER updates a session → 200 with updated data")
        void organizerUpdates() {
            String sessionId = given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions")
            .then()
                .extract().jsonPath().getString("[0].id");

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forOrganizer())
                .body("""
                    {
                      "title": "Updated Session Title",
                      "abstract": "Updated abstract.",
                      "level": "INTERMEDIATE",
                      "track": "Updated",
                      "speakerId": "spk-duke",
                      "startTime": "2026-10-16T15:00:00",
                      "durationMinutes": 90
                    }
                    """)
            .when()
                .put("/api/v1/sessions/" + sessionId)
            .then()
                .statusCode(200)
                .body("title", is("Updated Session Title"));
        }

        @Test
        @DisplayName("ORGANIZER deletes a session → 204, then GET → 404")
        void organizerDeletes() {
            String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + TestTokens.forOrganizer())
                .body(SESSION_BODY)
            .when()
                .post("/api/v1/sessions")
            .then()
                .statusCode(201)
                .extract().jsonPath().getString("id");

            given()
                .header("Authorization", "Bearer " + TestTokens.forOrganizer())
            .when()
                .delete("/api/v1/sessions/" + id)
            .then()
                .statusCode(204);

            // Verify it's gone
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions/" + id)
            .then()
                .statusCode(404);
        }
    }
}

