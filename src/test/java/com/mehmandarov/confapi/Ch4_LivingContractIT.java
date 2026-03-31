package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Pattern 4: The Living Contract — End-to-End</strong>
 * <p>
 * Proves: the OpenAPI spec is generated from code (not hand-written),
 * reflects all annotations, security schemes, tags, and OASFilter enrichments.
 * The spec IS the contract — if these tests pass, the contract is correct.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Ch4 IT — The Living Contract (OpenAPI)")
class Ch4_LivingContractIT {

    @Nested
    @DisplayName("Spec Metadata — title, version, and structure")
    class Metadata {

        @Test
        @DisplayName("OpenAPI spec has correct title and version from @OpenAPIDefinition")
        void specHasCorrectTitleAndVersion() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/openapi?format=json")
            .then()
                .statusCode(200)
                .body("info.title", is("Conference Session API"))
                .body("info.version", is("1.0.0"));
        }

        @Test
        @DisplayName("Spec includes both V1 and V2 session endpoints")
        void specIncludesBothVersions() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/openapi?format=json")
            .then()
                .statusCode(200)
                .body("paths", hasKey("/api/v1/sessions"))
                .body("paths", hasKey("/api/v2/sessions"));
        }

        @Test
        @DisplayName("Spec defines tags for all resource groups")
        void specDefinesTags() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/openapi?format=json")
            .then()
                .statusCode(200)
                .body("tags.name", hasItems("Sessions", "Speakers", "Rooms", "v2"));
        }
    }

    @Nested
    @DisplayName("Security Scheme — JWT is documented in the spec")
    class SecurityScheme {

        @Test
        @DisplayName("Spec declares 'jwt' as a Bearer HTTP security scheme")
        void jwtSecuritySchemeExists() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/openapi?format=json")
            .then()
                .statusCode(200)
                .body("components.securitySchemes.jwt.type", is("http"))
                .body("components.securitySchemes.jwt.scheme", is("bearer"));
        }
    }

    @Nested
    @DisplayName("OASFilter — programmatic enrichment of the spec")
    class OASFilter {

        @Test
        @DisplayName("OASFilter adds a 500 'Internal Server Error' response to all operations")
        void filterAdds500Response() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/openapi?format=json")
            .then()
                .statusCode(200)
                .body("paths.'/api/v1/sessions'.get.responses.'500'.description",
                      containsString("Internal Server Error"));
        }
    }
}
