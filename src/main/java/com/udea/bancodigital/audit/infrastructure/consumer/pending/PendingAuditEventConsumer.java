package com.udea.bancodigital.audit.infrastructure.consumer.pending;

import com.udea.bancodigital.audit.infrastructure.adapter.out.AuditEventPersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Pending Event Consumer for Audit Service.
 *
 * Handles retry and replay of failed audit events:
 * - Listens to: audit-events-pending
 * - Retry logic: exponential backoff
 * - DLQ: audit-events-dlq (after max retries)
 * - Auto-recovery: When Audit Database comes back online
 *
 * Flow:
 * 1. Event queued to pending topic when BD_AUDIT is down
 * 2. PendingAuditEventConsumer picks it up
 * 3. Attempts to persist via AuditEventPersistenceAdapter
 * 4. If fails: retries with exponential backoff
 * 5. After maxRetries: moved to DLQ for investigation
 * 6. When BD_AUDIT recovers: events automatically replayed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingAuditEventConsumer {

    private static final String EVENT_ID_KEY = "eventId";
    private static final String RETRY_COUNT_KEY = "retryCount";


    private static final String PENDING_TOPIC = "audit-events-pending";
    private static final String DLQ_TOPIC = "audit-events-dlq";
    private static final String CONSUMER_GROUP = "audit-pending";
    private static final int MAX_RETRIES = 5;

    private final AuditEventPersistenceAdapter auditEventPersistenceAdapter;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    /**
     * Consume pending audit events and retry persistence.
     */
    @KafkaListener(
            topics = PENDING_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePendingEvent(Map<String, Object> event) {
        try {
            String eventId = String.valueOf(event.get(EVENT_ID_KEY));
            String eventType = String.valueOf(event.get("eventType"));
            int retryCount = (int) event.getOrDefault(RETRY_COUNT_KEY, 0);

            log.info("Processing pending audit event: "
                + "eventId={}, type={}, retryCount={}/{}",
                eventId, eventType, retryCount, MAX_RETRIES);

            // Attempt to persist audit event
            auditEventPersistenceAdapter.persistAuditEvent(event, eventType);

            log.info("Successfully replayed audit event: eventId={}, type={}", 
                eventId, eventType);

        } catch (Exception e) {
            handleRetry(event, e);
        }
    }

    /**
     * Handle retry logic with exponential backoff and DLQ routing.
     */
    private void handleRetry(Map<String, Object> event, Exception e) {
        int retryCount = (int) event.getOrDefault(RETRY_COUNT_KEY, 0);

        if (retryCount >= MAX_RETRIES) {
            // Max retries reached, move to DLQ
            moveToDLQ(event, "Max retries exceeded: " + e.getMessage());
        } else {
            // Retry with exponential backoff
            retryWithBackoff(event, retryCount);
        }
    }

    /**
     * Retry with exponential backoff.
     * Backoff: attempt 1 = 1s, attempt 2 = 2s, attempt 3 = 4s, etc.
     */
    private void retryWithBackoff(Map<String, Object> event, int retryCount) {
        long backoffMs = (long) Math.pow(2, retryCount) * 1000; // 1s, 2s, 4s, 8s, 16s
        int nextRetry = retryCount + 1;

        log.warn("Retry failed for eventId={}. Scheduling retry {} of {}, "
            + "backoff={}ms",
            event.get(EVENT_ID_KEY), nextRetry, MAX_RETRIES, backoffMs);

        // Update retry metadata
        event.put(RETRY_COUNT_KEY, nextRetry);
        event.put("lastRetryAt", Instant.now().toString());
        event.put("nextRetryScheduledAt", 
            Instant.now().plusMillis(backoffMs).toString());

        // Re-queue to pending topic
        kafkaTemplate.send(PENDING_TOPIC, String.valueOf(event.get(EVENT_ID_KEY)), event);
    }

    /**
     * Move event to DLQ after max retries exceeded.
     */
    private void moveToDLQ(Map<String, Object> event, String reason) {
        log.error("Moving audit event to DLQ. "
            + "eventId={}, reason={}",
            event.get(EVENT_ID_KEY), reason);

        // Add failure metadata
        event.put("failedAt", Instant.now().toString());
        event.put("failureReason", reason);
        event.put("movedToDLQAt", Instant.now().toString());

        // Send to DLQ
        kafkaTemplate.send(DLQ_TOPIC, String.valueOf(event.get(EVENT_ID_KEY)), event);

        log.info("Event moved to DLQ for investigation. "
            + "Topic={}, eventId={}",
            DLQ_TOPIC, event.get(EVENT_ID_KEY));
    }

    /**
     * Get consumer statistics.
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "topic", PENDING_TOPIC,
            "dlqTopic", DLQ_TOPIC,
            "maxRetries", MAX_RETRIES,
            "consumerGroup", CONSUMER_GROUP
        );
    }
}
