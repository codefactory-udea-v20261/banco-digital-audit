package com.udea.bancodigital.audit.infrastructure.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.bancodigital.audit.infrastructure.entity.AuditEventEntity;
import com.udea.bancodigital.audit.infrastructure.repository.AuditEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPersistenceAdapter {

    private final AuditEventRepository auditEventRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "audit-database", fallbackMethod = "persistEventFallback")
    @Retry(name = "audit-database")
    public void persistAuditEvent(Map<String, Object> event, String eventType) {
        log.debug("Persisting audit event: type={}, eventId={}",
            eventType, event.get("eventId"));

        AuditEventEntity entity = AuditEventEntity.builder()
            .eventId(String.valueOf(event.getOrDefault("eventId", "")))
            .eventType(eventType)
            .aggregateId(String.valueOf(event.getOrDefault("aggregateId", "")))
            .correlationId(String.valueOf(event.getOrDefault("correlationId", "")))
            .userId(String.valueOf(event.getOrDefault("userId", "")))
            .sourceService(String.valueOf(event.getOrDefault("sourceService", "")))
            .occurredAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();

        try {
            entity.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event payload: {}", e.getMessage());
        }

        auditEventRepository.save(entity);

        log.info("Successfully persisted audit event: type={}, eventId={}",
            eventType, event.get("eventId"));
    }

    private void persistEventFallback(Map<String, Object> event, String eventType, Exception e) {
        log.warn("Audit database unavailable (circuit breaker OPEN). "
            + "Queueing event for async persistence. Type={}, Error: {}",
            eventType, e.getMessage());

        try {
            Map<String, Object> pendingEvent = new HashMap<>(event);
            pendingEvent.put("eventType", eventType);
            pendingEvent.put("originalEventId", event.get("eventId"));
            pendingEvent.put("timestamp", Instant.now().toString());
            pendingEvent.put("retryCount", 0);
            pendingEvent.put("reason", "Audit database unavailable");

            kafkaTemplate.send("audit-events-pending",
                String.valueOf(event.get("aggregateId")),
                pendingEvent);

            log.info("Queued pending audit event for aggregateId={}", event.get("aggregateId"));

        } catch (Exception kafkaError) {
            log.error("Failed to queue fallback audit event: {}", kafkaError.getMessage());
        }
    }

    public Map<String, Object> getStatus() {
        return Map.of(
            "circuitBreakerName", "audit-database",
            "status", "Use /actuator/health/audit-database for details"
        );
    }
}
