package com.udea.bancodigital.audit.infrastructure.consumer;

import com.udea.bancodigital.audit.infrastructure.adapter.out.AuditEventPersistenceAdapter;
import com.udea.bancodigital.shared.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @InjectMocks
    private EventConsumer eventConsumer;

    @Mock
    private AuditEventPersistenceAdapter auditEventPersistenceAdapter;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Test
    @DisplayName("Should process CustomerCreated event correctly")
    void shouldProcessCustomerCreated() {
        // Given
        DomainEvent event = createEvent("CustomerCreated", "agg-1");

        // When
        eventConsumer.consumeEvent(event, "banco-digital-events", 0, 100L);

        // Then
        verify(auditEventPersistenceAdapter).persistAuditEvent(mapCaptor.capture(), eq("CustomerCreated"));
        Map<String, Object> map = mapCaptor.getValue();
        assertThat(map).containsEntry("eventId", event.getEventId());
        assertThat(map).containsEntry("aggregateId", "agg-1");
        assertThat(map).containsEntry("eventType", "CustomerCreated");
    }

    @Test
    @DisplayName("Should process TransactionCompleted event correctly")
    void shouldProcessTransactionCompleted() {
        // Given
        DomainEvent event = createEvent("TransactionCompleted", "agg-2");

        // When
        eventConsumer.consumeEvent(event, "banco-digital-events", 0, 101L);

        // Then
        verify(auditEventPersistenceAdapter).persistAuditEvent(mapCaptor.capture(), eq("TransactionCompleted"));
        Map<String, Object> map = mapCaptor.getValue();
        assertThat(map).containsEntry("eventId", event.getEventId());
        assertThat(map).containsEntry("aggregateId", "agg-2");
        assertThat(map).containsEntry("eventType", "TransactionCompleted");
    }

    @Test
    @DisplayName("Should process AccountOpened event correctly")
    void shouldProcessAccountOpened() {
        // Given
        DomainEvent event = createEvent("AccountOpened", "agg-3");

        // When
        eventConsumer.consumeEvent(event, "banco-digital-events", 0, 102L);

        // Then
        verify(auditEventPersistenceAdapter).persistAuditEvent(mapCaptor.capture(), eq("AccountOpened"));
        Map<String, Object> map = mapCaptor.getValue();
        assertThat(map).containsEntry("eventId", event.getEventId());
        assertThat(map).containsEntry("aggregateId", "agg-3");
        assertThat(map).containsEntry("eventType", "AccountOpened");
    }

    @Test
    @DisplayName("Should handle unknown event type gracefully")
    void shouldHandleUnknownEventType() {
        // Given
        DomainEvent event = createEvent("UnknownEvent", "agg-4");

        // When
        eventConsumer.consumeEvent(event, "banco-digital-events", 0, 103L);

        // Then
        verify(auditEventPersistenceAdapter, never()).persistAuditEvent(any(), any());
    }

    @Test
    @DisplayName("Should handle persistence exception gracefully")
    void shouldHandlePersistenceException() {
        // Given
        DomainEvent event = createEvent("CustomerCreated", "agg-5");
        doThrow(new RuntimeException("Database down")).when(auditEventPersistenceAdapter)
                .persistAuditEvent(any(), any());

        // When
        eventConsumer.consumeEvent(event, "banco-digital-events", 0, 104L);

        // Then
        verify(auditEventPersistenceAdapter).persistAuditEvent(any(), eq("CustomerCreated"));
        // Exception caught and logged, method finishes normally
    }

    private DomainEvent createEvent(String eventType, String aggregateId) {
        return DomainEvent.builder()
                .eventId("evt-" + aggregateId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .userId("user-1")
                .occurredAt(LocalDateTime.now())
                .version(1)
                .build();
    }
}
