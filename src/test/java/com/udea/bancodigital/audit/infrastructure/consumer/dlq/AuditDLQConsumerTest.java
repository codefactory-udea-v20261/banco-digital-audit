package com.udea.bancodigital.audit.infrastructure.consumer.dlq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class AuditDLQConsumerTest {

    @InjectMocks
    private AuditDLQConsumer consumer;

    @Test
    @DisplayName("Should process DLQ event without throwing exception")
    void shouldProcessDLQEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("eventType", "CustomerCreated");
        event.put("retryCount", 3);
        event.put("failureReason", "Database timeout");

        // The method only logs currently, so we just verify it doesn't throw exceptions
        assertThatCode(() -> consumer.consumeDLQEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should process DLQ event when retry count is missing")
    void shouldProcessDLQEventWhenRetryCountMissing() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-456");
        event.put("eventType", "AccountOpened");
        // missing retryCount
        event.put("failureReason", "Network error");

        assertThatCode(() -> consumer.consumeDLQEvent(event))
                .doesNotThrowAnyException();
    }
}
