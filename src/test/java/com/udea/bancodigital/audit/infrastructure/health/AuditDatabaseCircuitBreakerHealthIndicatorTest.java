package com.udea.bancodigital.audit.infrastructure.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditDatabaseCircuitBreakerHealthIndicatorTest {

    @InjectMocks
    private AuditDatabaseCircuitBreakerHealthIndicator healthIndicator;

    @Mock
    private CircuitBreakerRegistry registry;

    @Test
    @DisplayName("Should return UP when circuit breaker is CLOSED")
    void shouldReturnUpWhenClosed() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        
        when(registry.circuitBreaker("audit-database")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(cb.getMetrics()).thenReturn(metrics);
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("state", "CLOSED");
    }

    @Test
    @DisplayName("Should return OUT_OF_SERVICE when circuit breaker is OPEN")
    void shouldReturnOutOfServiceWhenOpen() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        
        when(registry.circuitBreaker("audit-database")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.OPEN);
        when(cb.getMetrics()).thenReturn(metrics);
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("state", "OPEN");
    }

    @Test
    @DisplayName("Should return DOWN when circuit breaker is HALF_OPEN")
    void shouldReturnDownWhenHalfOpen() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        
        when(registry.circuitBreaker("audit-database")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.HALF_OPEN);
        when(cb.getMetrics()).thenReturn(metrics);
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("state", "HALF_OPEN");
    }

    @Test
    @DisplayName("Should return DOWN when circuit breaker throws exception")
    void shouldReturnDownOnException() {
        when(registry.circuitBreaker("audit-database")).thenThrow(new RuntimeException("Registry error"));
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Circuit breaker 'audit-database' not found");
        assertThat(health.getDetails()).containsEntry("reason", "Registry error");
    }
    
    @Test
    @DisplayName("Should return UP for METRICS_ONLY state")
    void shouldReturnUpWhenMetricsOnly() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        
        when(registry.circuitBreaker("audit-database")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.METRICS_ONLY);
        when(cb.getMetrics()).thenReturn(metrics);
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("state", "METRICS_ONLY");
    }
    
    @Test
    @DisplayName("Should return UP for DISABLED state")
    void shouldReturnUpWhenDisabled() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        
        when(registry.circuitBreaker("audit-database")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.DISABLED);
        when(cb.getMetrics()).thenReturn(metrics);
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("state", "DISABLED");
    }
    
    @Test
    @DisplayName("Should return UNKNOWN for FORCED_OPEN state")
    void shouldReturnUnknownWhenForcedOpen() {
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);
        
        when(registry.circuitBreaker("audit-database")).thenReturn(cb);
        when(cb.getState()).thenReturn(CircuitBreaker.State.FORCED_OPEN);
        when(cb.getMetrics()).thenReturn(metrics);
        
        Health health = healthIndicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("state", "FORCED_OPEN");
    }
}
