package com.udea.bancodigital.audit.infrastructure.consumer;

import com.udea.bancodigital.audit.infrastructure.adapter.out.AuditEventPersistenceAdapter;
import com.udea.bancodigital.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer for domain events.
 * Listens to the main event topic and logs all events for audit trail.
 * 
 * With Circuit Breaker protection: if audit database fails,
 * events are queued to Kafka for later retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private static final String EVENTS_TOPIC = "banco-digital-events";
    private static final String CONSUMER_GROUP = "audit-service";

    private final AuditEventPersistenceAdapter auditEventPersistenceAdapter;

    /**
     * Consumes events from the main event bus and processes them with resilience.
     */
    @KafkaListener(
            topics = EVENTS_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvent(
            @Payload DomainEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received event: {} (id: {}, type: {}) from topic: {}, partition: {}, offset: {}",
                    event.getEventType(),
                    event.getEventId(),
                    event.getEventType(),
                    topic,
                    partition,
                    offset);

            processEvent(event);

            log.debug("Successfully processed event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process event {}: {}", event.getEventId(), e.getMessage(), e);
            // Resilience4j circuit breaker handles fallback via Kafka queueing
        }
    }

    /**
     * Routes the event to the appropriate handler based on event type.
     */
    private void processEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "CustomerCreated":
                handleCustomerCreated(event);
                break;
            case "TransactionCompleted":
                handleTransactionCompleted(event);
                break;
            case "AccountOpened":
                handleAccountOpened(event);
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    /**
     * Handle CustomerCreated event.
     * Log the customer creation for audit trail with circuit breaker protection.
     */
    private void handleCustomerCreated(DomainEvent event) {
        log.info("[AUDIT] Customer created - aggregateId: {}, userId: {}",
                event.getAggregateId(),
                event.getUserId());
        
        // Persist to audit log with circuit breaker protection
        Map<String, Object> auditEvent = eventToMap(event);
        auditEventPersistenceAdapter.persistAuditEvent(auditEvent, "CustomerCreated");
    }

    /**
     * Handle TransactionCompleted event.
     * Log all transactions for regulatory compliance with circuit breaker protection.
     */
    private void handleTransactionCompleted(DomainEvent event) {
        log.info("[AUDIT] Transaction completed - transactionId: {}, userId: {}",
                event.getAggregateId(),
                event.getUserId());
        
        // Persist to audit log with circuit breaker protection
        Map<String, Object> auditEvent = eventToMap(event);
        auditEventPersistenceAdapter.persistAuditEvent(auditEvent, "TransactionCompleted");
    }

    /**
     * Handle AccountOpened event.
     * Log account opening for audit trail with circuit breaker protection.
     */
    private void handleAccountOpened(DomainEvent event) {
        log.info("[AUDIT] Account opened - aggregateId: {}, userId: {}",
                event.getAggregateId(),
                event.getUserId());
        
        // Persist to audit log with circuit breaker protection
        Map<String, Object> auditEvent = eventToMap(event);
        auditEventPersistenceAdapter.persistAuditEvent(auditEvent, "AccountOpened");
    }

    /**
     * Convert DomainEvent to Map for persistence.
     */
    private Map<String, Object> eventToMap(DomainEvent event) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventId", event.getEventId());
        eventMap.put("aggregateId", event.getAggregateId());
        eventMap.put("eventType", event.getEventType());
        eventMap.put("userId", event.getUserId());
        eventMap.put("timestamp", event.getOccurredAt());
        eventMap.put("version", event.getVersion());
        return eventMap;
    }
}
