package com.udea.bancodigital.audit.infrastructure.consumer.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dead Letter Queue Consumer for Audit Service.
 *
 * Monitors and alerts on failed audit events that exhausted retries.
 * 
 * In production, should integrate with:
 * - Slack/PagerDuty for critical alerts
 * - Database for DLQ event persistence (audit trail)
 * - Metrics for tracking DLQ growth
 * - Manual intervention workflow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditDLQConsumer {

    private static final String DLQ_TOPIC = "audit-events-dlq";
    private static final String CONSUMER_GROUP = "audit-dlq";

    /**
     * Consume audit DLQ events.
     * These are audit events that failed after max retries.
     */
    @KafkaListener(
            topics = DLQ_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDLQEvent(Map<String, Object> event) {
        String eventId = String.valueOf(event.get("eventId"));
        String eventType = String.valueOf(event.get("eventType"));
        int retryCount = (int) event.getOrDefault("retryCount", 0);
        String reason = String.valueOf(event.get("failureReason"));

        log.error("AUDIT DLQ EVENT - eventId={}, type={}, retries={}, reason={}",
            eventId, eventType, retryCount, reason);

        // CRITICAL: Audit failures must be investigated immediately
        // In production: send CRITICAL alert to PagerDuty
        // alertingService.sendCriticalAlert(
        //     "Audit Event Failed",
        //     String.format("EventId: %s, Type: %s, Retries: %d, Reason: %s",
        //         eventId, eventType, retryCount, reason)
        // );

        // In production: persist for compliance audit trail
        // auditDLQPersistenceService.recordFailedEvent(event);

        // Increment metrics
        // meterRegistry.counter("dlq.audit-events").increment();
    }
}
