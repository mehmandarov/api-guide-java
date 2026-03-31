/**
 * <strong>Pattern 3: The Lens (Observability)</strong>
 * <p>
 * Implementing tracing filters and health checks to understand exactly
 * what is happening during a request.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link com.mehmandarov.confapi.observability.TracingFilter} — OpenTelemetry span enrichment</li>
 *   <li>{@link com.mehmandarov.confapi.observability.CorrelationIdFilter} — {@code X-Request-Id} propagation</li>
 *   <li>{@link com.mehmandarov.confapi.observability.StartupHealthCheck} — {@code @Liveness} probe</li>
 *   <li>{@link com.mehmandarov.confapi.observability.DataReadinessCheck} — {@code @Readiness} probe</li>
 * </ul>
 */
package com.mehmandarov.confapi.observability;

