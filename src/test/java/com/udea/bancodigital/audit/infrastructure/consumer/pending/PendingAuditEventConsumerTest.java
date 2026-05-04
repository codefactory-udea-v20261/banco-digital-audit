package com.udea.bancodigital.audit.infrastructure.consumer.pending;

import com.udea.bancodigital.audit.infrastructure.adapter.out.AuditEventPersistenceAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingAuditEventConsumerTest {

    @InjectMocks
    private PendingAuditEventConsumer consumer;

    @Mock
    private AuditEventPersistenceAdapter adapter;

    @Mock
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Test
    @DisplayName("Should successfully replay event")
    void shouldSuccessfullyReplay() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("eventType", "CustomerCreated");

        consumer.consumePendingEvent(event);

        verify(adapter).persistAuditEvent(event, "CustomerCreated");
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should retry with backoff when persistence fails and under max retries")
    void shouldRetryWithBackoff() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("eventType", "CustomerCreated");
        event.put("retryCount", 2);

        doThrow(new RuntimeException("DB down")).when(adapter).persistAuditEvent(any(), any());

        consumer.consumePendingEvent(event);

        verify(kafkaTemplate).send(eq("audit-events-pending"), eq("evt-123"), mapCaptor.capture());
        Map<String, Object> requeuedEvent = mapCaptor.getValue();
        assertThat(requeuedEvent.get("retryCount")).isEqualTo(3);
        assertThat(requeuedEvent).containsKey("nextRetryScheduledAt");
    }

    @Test
    @DisplayName("Should move to DLQ when persistence fails and max retries reached")
    void shouldMoveToDLQ() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("eventType", "CustomerCreated");
        event.put("retryCount", 5);

        doThrow(new RuntimeException("DB down")).when(adapter).persistAuditEvent(any(), any());

        consumer.consumePendingEvent(event);

        verify(kafkaTemplate).send(eq("audit-events-dlq"), eq("evt-123"), mapCaptor.capture());
        Map<String, Object> dlqEvent = mapCaptor.getValue();
        assertThat(dlqEvent).containsKey("failureReason");
        assertThat(dlqEvent).containsKey("movedToDLQAt");
    }

    @Test
    @DisplayName("Should return consumer stats")
    void shouldReturnConsumerStats() {
        Map<String, Object> stats = consumer.getStats();

        assertThat(stats).containsEntry("topic", "audit-events-pending");
        assertThat(stats).containsEntry("dlqTopic", "audit-events-dlq");
        assertThat(stats).containsEntry("maxRetries", 5);
        assertThat(stats).containsEntry("consumerGroup", "audit-pending");
    }
}
