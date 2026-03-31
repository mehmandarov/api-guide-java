package com.mehmandarov.confapi;

import com.mehmandarov.confapi.support.ConfApiExtension;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * <strong>Pattern 3: The Lens — End-to-End</strong>
 * <p>
 * Proves: health endpoints report system state accurately, and every
 * response carries a correlation ID for end-to-end traceability.
 */
@ExtendWith(ConfApiExtension.class)
@DisplayName("Ch3 IT — The Lens (Observability)")
class Ch3_ObservabilityIT {

    @Nested
    @DisplayName("Health Checks — Kubernetes readiness and liveness probes")
    class HealthChecks {

        @Test
        @DisplayName("Liveness probe returns UP → process is alive")
        void livenessUp() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/health/live")
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.find { it.name == 'confapi-live' }.status", is("UP"));
        }

        @Test
        @DisplayName("Readiness probe returns UP with sessionCount → data is loaded")
        void readinessUpWithData() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/health/ready")
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.find { it.name == 'confapi-data-ready' }.status", is("UP"))
                .body("checks.find { it.name == 'confapi-data-ready' }.data.sessionCount",
                      greaterThanOrEqualTo(3));
        }
    }

    @Nested
    @DisplayName("Correlation ID — every request/response carries X-Request-Id")
    class CorrelationId {

        @Test
        @DisplayName("Server auto-generates X-Request-Id when client doesn't send one")
        void autoGeneratesCorrelationId() {
            given()
                .accept(ContentType.JSON)
            .when()
                .get("/api/v1/sessions")
            .then()
                .statusCode(200)
                .header("X-Request-Id", notNullValue());
        }

        @Test
        @DisplayName("Server echoes client-provided X-Request-Id back → end-to-end tracing")
        void echosClientCorrelationId() {
            String clientId = "client-trace-abc-123";

            given()
                .accept(ContentType.JSON)
                .header("X-Request-Id", clientId)
            .when()
                .get("/api/v1/sessions")
            .then()
                .statusCode(200)
                .header("X-Request-Id", is(clientId));
        }
    }
}
