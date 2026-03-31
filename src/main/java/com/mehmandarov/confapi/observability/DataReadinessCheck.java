package com.mehmandarov.confapi.observability;

import com.mehmandarov.confapi.repository.SessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness probe — verifies the application data is loaded and ready
 * to serve traffic.
 * <p>
 * Kubernetes uses this to decide whether to route traffic to the pod.
 */
@Readiness
@ApplicationScoped
public class DataReadinessCheck implements HealthCheck {

    @Inject
    SessionRepository sessionRepo;

    @Override
    public HealthCheckResponse call() {
        int count = sessionRepo.findAll().size();
        return HealthCheckResponse.named("confapi-data-ready")
                .withData("sessionCount", count)
                .status(count > 0)
                .build();
    }
}

