package com.mehmandarov.confapi.unit;

import com.mehmandarov.confapi.observability.CorrelationIdFilter;
import com.mehmandarov.confapi.observability.DataReadinessCheck;
import com.mehmandarov.confapi.observability.StartupHealthCheck;
import com.mehmandarov.confapi.repository.SessionRepository;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <strong>Pattern 3: The Lens (Observability)</strong>
 * <p>
 * Tests prove: correlation IDs are generated/preserved correctly,
 * and health checks accurately reflect system state.
 */
@DisplayName("Ch3 — The Lens (Observability)")
class Ch3_ObservabilityTest {

    @BeforeAll
    static void registerHealthProvider() {
        // MicroProfile Health uses an SPI (HealthCheckResponseProvider) that runtimes
        // register automatically. In plain unit tests (no container), we register
        // SmallRye Health's reference implementation manually.
        HealthCheckResponse.setResponseProvider(new io.smallrye.health.ResponseProvider());
    }

    @Nested
    @DisplayName("Correlation ID — every request gets a traceable identifier")
    class CorrelationId {

        @Test
        @DisplayName("Header constant matches 'X-Request-Id'")
        void headerNameIsCorrect() {
            assertEquals("X-Request-Id", CorrelationIdFilter.HEADER_NAME);
        }

        @Test
        @DisplayName("Property name is 'correlation.id' — available to all filters")
        void propertyNameIsCorrect() {
            assertEquals("correlation.id", CorrelationIdFilter.PROPERTY_NAME);
        }

        @Test
        @DisplayName("Filter implements both request and response interfaces (echoes ID back)")
        void implementsBothFilterInterfaces() {
            CorrelationIdFilter filter = new CorrelationIdFilter();
            assertInstanceOf(
                    jakarta.ws.rs.container.ContainerRequestFilter.class, filter,
                    "Must intercept requests to generate/capture the ID");
            assertInstanceOf(
                    jakarta.ws.rs.container.ContainerResponseFilter.class, filter,
                    "Must intercept responses to echo the ID back to the client");
        }
    }

    @Nested
    @DisplayName("Health Checks — Kubernetes readiness/liveness signals")
    class HealthChecks {

        @Test
        @DisplayName("Liveness check always reports UP (the process is alive)")
        void livenessAlwaysUp() {
            StartupHealthCheck check = new StartupHealthCheck();
            HealthCheckResponse resp = check.call();

            assertEquals("confapi-live", resp.getName());
            assertEquals(HealthCheckResponse.Status.UP, resp.getStatus());
        }

        @Test
        @DisplayName("Readiness reports UP when data is loaded (sessions > 0)")
        void readinessUpWhenDataLoaded() throws Exception {
            SessionRepository repo = new SessionRepository();
            repo.init(); // Load seed data (3 sessions)

            DataReadinessCheck check = new DataReadinessCheck();
            injectRepo(check, repo);

            HealthCheckResponse resp = check.call();
            assertEquals("confapi-data-ready", resp.getName());
            assertEquals(HealthCheckResponse.Status.UP, resp.getStatus());
            assertTrue(resp.getData().isPresent(), "Should include sessionCount data");
        }

        @Test
        @DisplayName("Readiness reports DOWN when no data is loaded")
        void readinessDownWhenEmpty() throws Exception {
            SessionRepository emptyRepo = new SessionRepository();
            // Don't call init() — repo has zero sessions

            DataReadinessCheck check = new DataReadinessCheck();
            injectRepo(check, emptyRepo);

            HealthCheckResponse resp = check.call();
            assertEquals(HealthCheckResponse.Status.DOWN, resp.getStatus(),
                    "Empty repo means NOT ready to serve traffic");
        }

        private void injectRepo(DataReadinessCheck check, SessionRepository repo) throws Exception {
            Field f = DataReadinessCheck.class.getDeclaredField("sessionRepo");
            f.setAccessible(true);
            f.set(check, repo);
        }
    }
}

