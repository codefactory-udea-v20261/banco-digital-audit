package com.udea.bancodigital.shared.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    @DisplayName("Should create DomainEvent using Builder correctly")
    void testDomainEventBuilder() {
        LocalDateTime now = LocalDateTime.now();
        DomainEvent event = DomainEvent.builder()
                .eventId("evt-001")
                .eventType("TEST_EVENT")
                .aggregateId("agg-001")
                .correlationId("corr-001")
                .sagaId("saga-001")
                .occurredAt(now)
                .sourceService("test-service")
                .version(1)
                .userId("user-001")
                .build();

        assertThat(event.getEventId()).isEqualTo("evt-001");
        assertThat(event.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(event.getAggregateId()).isEqualTo("agg-001");
        assertThat(event.getCorrelationId()).isEqualTo("corr-001");
        assertThat(event.getSagaId()).isEqualTo("saga-001");
        assertThat(event.getOccurredAt()).isEqualTo(now);
        assertThat(event.getSourceService()).isEqualTo("test-service");
        assertThat(event.getVersion()).isEqualTo(1);
        assertThat(event.getUserId()).isEqualTo("user-001");
    }

    @Test
    @DisplayName("Should create DomainEvent using NoArgsConstructor and Setters correctly")
    void testDomainEventSetters() {
        LocalDateTime now = LocalDateTime.now();
        DomainEvent event = new DomainEvent();
        event.setEventId("evt-002");
        event.setEventType("TEST_EVENT_2");
        event.setAggregateId("agg-002");
        event.setCorrelationId("corr-002");
        event.setSagaId("saga-002");
        event.setOccurredAt(now);
        event.setSourceService("test-service-2");
        event.setVersion(2);
        event.setUserId("user-002");

        assertThat(event.getEventId()).isEqualTo("evt-002");
        assertThat(event.getEventType()).isEqualTo("TEST_EVENT_2");
        assertThat(event.getAggregateId()).isEqualTo("agg-002");
        assertThat(event.getCorrelationId()).isEqualTo("corr-002");
        assertThat(event.getSagaId()).isEqualTo("saga-002");
        assertThat(event.getOccurredAt()).isEqualTo(now);
        assertThat(event.getSourceService()).isEqualTo("test-service-2");
        assertThat(event.getVersion()).isEqualTo(2);
        assertThat(event.getUserId()).isEqualTo("user-002");
    }
}
