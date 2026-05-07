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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPersistenceAdapter {

    private static final String EVENT_ID = "eventId";
    private static final String AGGREGATE_ID = "aggregateId";

    private final AuditEventRepository auditEventRepository;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating hash", e);
        }
    }

    @CircuitBreaker(name = "audit-database", fallbackMethod = "persistEventFallback")
    @Retry(name = "audit-database")
    public void persistAuditEvent(Map<String, Object> event, String eventType) {
        log.debug("Persisting audit event: type={}, eventId={}",
            eventType, event.get(EVENT_ID));

        String payloadStr = null;
        try {
            payloadStr = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event payload: {}", e.getMessage());
        }

        // Get previous hash for chaining
        String previousHash = null;
        Optional<AuditEventEntity> lastEvent = auditEventRepository.findTopByOrderByCreatedAtDesc();
        if (lastEvent.isPresent()) {
            previousHash = lastEvent.get().getCurrentHash();
        }

        String currentHash = generateHash((previousHash != null ? previousHash : "") + payloadStr);

        AuditEventEntity entity = AuditEventEntity.builder()
            .eventId(String.valueOf(event.getOrDefault(EVENT_ID, "")))
            .eventType(eventType)
            .aggregateId(String.valueOf(event.getOrDefault(AGGREGATE_ID, "")))
            .correlationId(String.valueOf(event.getOrDefault("correlationId", "")))
            .userId(String.valueOf(event.getOrDefault("userId", "")))
            .sourceService(String.valueOf(event.getOrDefault("sourceService", "")))
            .occurredAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .payload(payloadStr)
            .previousHash(previousHash)
            .currentHash(currentHash)
            .build();

        auditEventRepository.save(entity);

        log.info("Successfully persisted audit event: type={}, eventId={}",
            eventType, event.get(EVENT_ID));
    }

    public void persistEventFallback(Map<String, Object> event, String eventType, Exception e) {
        log.warn("Audit database unavailable (circuit breaker OPEN). "
            + "Queueing event for async persistence. Type={}, Error: {}",
            eventType, e.getMessage());

        try {
            Map<String, Object> pendingEvent = new HashMap<>(event);
            pendingEvent.put("eventType", eventType);
            pendingEvent.put("originalEventId", event.get(EVENT_ID));
            pendingEvent.put("timestamp", Instant.now().toString());
            pendingEvent.put("retryCount", 0);
            pendingEvent.put("reason", "Audit database unavailable");

            kafkaTemplate.send("audit-events-pending",
                String.valueOf(event.get(AGGREGATE_ID)),
                pendingEvent);

            log.info("Queued pending audit event for aggregateId={}", event.get(AGGREGATE_ID));

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
