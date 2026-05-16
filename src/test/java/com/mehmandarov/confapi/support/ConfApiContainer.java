package com.mehmandarov.confapi.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Singleton Testcontainer that runs the Conference API.
 * <p>
 * The image is built from the Maven build output (e.g. {@code target/quarkus-app/}
 * for Quarkus, a WAR for Liberty, etc.). Because the container is shared across
 * all IT classes, it starts once per test run — keeping the feedback loop fast.
 * <p>
 * <strong>Runtime-agnostic by design:</strong> swap the {@code buildImage()}
 * implementation (via {@code -Druntime.profile=liberty}) to test against
 * Open Liberty, Helidon, or any other MicroProfile runtime. The IT tests
 * themselves don't change — only the container image does.
 * <p>
 * <strong>Trade-off: startup time vs. test isolation.</strong>
 * A single shared container is fast (~3 s startup amortized across 34 tests)
 * but means tests share mutable state (e.g. a created session is visible to
 * later tests). If full isolation is required, replace the singleton with a
 * per-class container — at the cost of ~3 s per IT class.
 */
public class ConfApiContainer extends GenericContainer<ConfApiContainer> {

    private static final int HTTP_PORT = 8080;
    private static ConfApiContainer instance;

    private ConfApiContainer(ImageFromDockerfile image) {
        super(image);
        withExposedPorts(HTTP_PORT);

        // Disable OpenTelemetry — no collector running during tests.
        // OTEL_SDK_DISABLED is the standard OpenTelemetry env var (works on any runtime).
        withEnv("OTEL_SDK_DISABLED", "true");

        waitingFor(
                Wait.forHttp("/api/v1/sessions")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(30)));
    }

    /** Lazy singleton — starts the container on first access, reuses afterwards. */
    public static synchronized ConfApiContainer getInstance() {
        if (instance == null) {
            instance = new ConfApiContainer(buildImage());
            instance.start();
        }
        return instance;
    }

    /** Base URL for REST Assured (e.g. {@code http://localhost:32789}). */
    public String baseUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }

    // ── Image builders — one per runtime ───────────────────────────
    //
    // Dockerfiles live under docker/<runtime>/Dockerfile.it so they get
    // proper syntax highlighting, linting, and diffs (instead of being
    // embedded as Java text blocks).

    private static ImageFromDockerfile buildImage() {
        String runtime = System.getProperty("runtime.profile", "quarkus");
        return switch (runtime) {
            case "quarkus" -> quarkusImage();
            case "liberty" -> libertyImage();
            case "helidon" -> helidonImage();
            default -> throw new IllegalArgumentException(
                    "Unknown runtime.profile: " + runtime
                            + " (supported: quarkus, liberty, helidon)");
        };
    }

    private static ImageFromDockerfile quarkusImage() {
        return new ImageFromDockerfile("confapi-it", false)
                .withFileFromPath("Dockerfile",
                        Path.of("docker/quarkus/Dockerfile.it"))
                .withFileFromPath("quarkus-app",
                        Path.of("target/quarkus-app"));
    }

    private static ImageFromDockerfile libertyImage() {
        return new ImageFromDockerfile("confapi-it-liberty", false)
                .withFileFromPath("Dockerfile",
                        Path.of("docker/liberty/Dockerfile.it"))
                .withFileFromPath("server.xml",
                        Path.of("src/main/liberty/config/server.xml"))
                .withFileFromPath("confapi.war",
                        Path.of("target/confapi.war"));
    }

    private static ImageFromDockerfile helidonImage() {
        return new ImageFromDockerfile("confapi-it-helidon", false)
                .withFileFromPath("Dockerfile",
                        Path.of("docker/helidon/Dockerfile.it"))
                .withFileFromPath("confapi.jar",
                        Path.of("target/confapi.jar"));
    }
}
