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
                .withDetail("state", "CLOSED")
                .withDetail("description", "Circuit breaker is healthy, calls passing through");
        } else if (state == CircuitBreaker.State.OPEN) {
            builder = Health.outOfService()
                .withDetail("state", "OPEN")
                .withDetail("description", "Circuit breaker is OPEN, failing fast. Audit database is likely DOWN");
        } else if (state == CircuitBreaker.State.HALF_OPEN) {
            builder = Health.down()
                .withDetail("state", "HALF_OPEN")
                .withDetail("description", "Circuit breaker testing recovery, limited calls allowed");
        } else if (state == CircuitBreaker.State.METRICS_ONLY) {
            builder = Health.up()
                .withDetail("state", "METRICS_ONLY")
                .withDetail("description", "Circuit breaker in metrics-only mode");
        } else if (state == CircuitBreaker.State.DISABLED) {
            builder = Health.up()
                .withDetail("state", "DISABLED")
                .withDetail("description", "Circuit breaker is disabled");
        } else {
            builder = Health.unknown()
                .withDetail("state", state.toString());
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
