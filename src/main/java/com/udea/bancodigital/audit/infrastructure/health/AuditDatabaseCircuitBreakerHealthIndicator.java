package com.udea.bancodigital.audit.infrastructure.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditDatabaseCircuitBreakerHealthIndicator implements HealthIndicator {

    private static final String STATE_KEY = "state";
    private static final String DESCRIPTION_KEY = "description";


    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry
                .circuitBreaker("audit-database");

            return mapCircuitBreakerStateToHealth(circuitBreaker);

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Circuit breaker 'audit-database' not found")
                .withDetail("reason", e.getMessage())
                .build();
        }
    }

    private Health mapCircuitBreakerStateToHealth(CircuitBreaker circuitBreaker) {
        CircuitBreaker.State state = circuitBreaker.getState();
        var metrics = circuitBreaker.getMetrics();

        Health.Builder builder;
        if (state == CircuitBreaker.State.CLOSED) {
            builder = Health.up()
                .withDetail(STATE_KEY, "CLOSED")
                .withDetail(DESCRIPTION_KEY, "Circuit breaker is healthy, calls passing through");
        } else if (state == CircuitBreaker.State.OPEN) {
            builder = Health.outOfService()
                .withDetail(STATE_KEY, "OPEN")
                .withDetail(DESCRIPTION_KEY, "Circuit breaker is OPEN, failing fast. Audit database is likely DOWN");
        } else if (state == CircuitBreaker.State.HALF_OPEN) {
            builder = Health.down()
                .withDetail(STATE_KEY, "HALF_OPEN")
                .withDetail(DESCRIPTION_KEY, "Circuit breaker testing recovery, limited calls allowed");
        } else if (state == CircuitBreaker.State.METRICS_ONLY) {
            builder = Health.up()
                .withDetail(STATE_KEY, "METRICS_ONLY")
                .withDetail(DESCRIPTION_KEY, "Circuit breaker in metrics-only mode");
        } else if (state == CircuitBreaker.State.DISABLED) {
            builder = Health.up()
                .withDetail(STATE_KEY, "DISABLED")
                .withDetail(DESCRIPTION_KEY, "Circuit breaker is disabled");
        } else {
            builder = Health.unknown()
                .withDetail(STATE_KEY, state.toString());
        }

        builder
            .withDetail("failureRate", String.format("%.2f%%", metrics.getFailureRate()))
            .withDetail("totalCalls", metrics.getNumberOfBufferedCalls())
            .withDetail("failedCalls", metrics.getNumberOfFailedCalls())
            .withDetail("successfulCalls", metrics.getNumberOfSuccessfulCalls())
            .withDetail("slowCalls", metrics.getNumberOfSlowCalls())
            .withDetail("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));

        return builder.build();
    }
}
