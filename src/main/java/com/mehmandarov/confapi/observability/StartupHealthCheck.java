package com.mehmandarov.confapi.observability;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Liveness probe — confirms the application process is running.
 * <p>
 * In a real deployment, this is what Kubernetes uses to decide
 * whether to restart the pod.
 */
@Liveness
@ApplicationScoped
public class StartupHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("confapi-live")
                .withData("javaVersion", Runtime.version().toString())
                .up()
                .build();
    }
}

