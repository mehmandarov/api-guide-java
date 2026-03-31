package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Pattern 5: The Evolution — End-to-End</strong>
 * <p>
 * Proves: V1 and V2 coexist, URI-based versioning works, and
 * header-based versioning transparently routes to the correct version.
 * Clients can upgrade at their own pace.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Ch5 IT — The Evolution (API Versioning)")
class Ch5_EvolutionIT {

    @Nested
    @DisplayName("URI-Based Versioning — /api/v1/ vs /api/v2/")
    class UriVersioning {

        @Test
        @DisplayName("V1 returns flat DTOs with speakerId (string FK)")
        void v1ReturnsFlatDtos() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions")
            .then()
                .statusCode(200)
                .body("[0].speakerId", notNullValue())
                .body("[0].speaker", nullValue());
        }

        @Test
        @DisplayName("V2 returns enriched DTOs with embedded speaker object")
        void v2ReturnsEnrichedDtos() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v2/sessions")
            .then()
                .statusCode(200)
                .body("[0].speaker", notNullValue())
                .body("[0].speaker.name", notNullValue())
                .body("[0].speaker.id", notNullValue());
        }

        @Test
        @DisplayName("V2 single session includes embedded room when assigned")
        void v2IncludesRoom() {
            String sessionId = given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v2/sessions")
            .then()
                .statusCode(200)
                .extract().jsonPath().getString("find { it.room != null }.id");

            if (sessionId != null) {
                given()
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/v2/sessions/" + sessionId)
                .then()
                    .statusCode(200)
                    .body("room.name", notNullValue())
                    .body("room.building", notNullValue());
            }
        }
    }

    @Nested
    @DisplayName("Header-Based Versioning — transparent routing via X-API-Version")
    class HeaderVersioning {

        @Test
        @DisplayName("X-API-Version: 2 → routes /api/sessions to V2 (enriched)")
        void xApiVersionRoutesToV2() {
            given()
                .accept(ContentType.JSON)
                .header("X-API-Version", "2")
            .when()
                .get("/api/sessions")
            .then()
                .statusCode(200)
                .body("[0].speaker", notNullValue());
        }

        @Test
        @DisplayName("X-API-Version: 1 → routes /api/sessions to V1 (flat)")
        void xApiVersionRoutesToV1() {
            given()
                .accept(ContentType.JSON)
                .header("X-API-Version", "1")
            .when()
                .get("/api/sessions")
            .then()
                .statusCode(200)
                .body("[0].speakerId", notNullValue())
                .body("[0].speaker", nullValue());
        }

        @Test
        @DisplayName("Accept: application/json; version=2 → routes to V2")
        void acceptParamRoutesToV2() {
            given()
                .header("Accept", "application/json; version=2")
            .when()
                .get("/api/sessions")
            .then()
                .statusCode(200)
                .body("[0].speaker", notNullValue());
        }
    }
}
