package com.udea.bancodigital.audit.infrastructure.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.bancodigital.audit.infrastructure.entity.AuditEventEntity;
import com.udea.bancodigital.audit.infrastructure.repository.AuditEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventPersistenceAdapterTest {

    @InjectMocks
    private AuditEventPersistenceAdapter adapter;

    @Mock
    private AuditEventRepository repository;

    @Mock
    private KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<AuditEventEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Test
    @DisplayName("Should persist event successfully")
    void shouldPersistEvent() throws JsonProcessingException {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("aggregateId", "agg-123");
        
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventId\":\"evt-123\"}");

        adapter.persistAuditEvent(event, "CustomerCreated");

        verify(repository).save(entityCaptor.capture());
        AuditEventEntity entity = entityCaptor.getValue();
        assertThat(entity.getEventId()).isEqualTo("evt-123");
        assertThat(entity.getEventType()).isEqualTo("CustomerCreated");
        assertThat(entity.getPayload()).isEqualTo("{\"eventId\":\"evt-123\"}");
    }

    @Test
    @DisplayName("Should handle JSON serialization error")
    void shouldHandleJsonError() throws JsonProcessingException {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");

        when(objectMapper.writeValueAsString(event)).thenThrow(mock(JsonProcessingException.class));

        adapter.persistAuditEvent(event, "CustomerCreated");

        verify(repository).save(entityCaptor.capture());
        AuditEventEntity entity = entityCaptor.getValue();
        assertThat(entity.getPayload()).isNull(); // Should be null on serialization failure
    }

    @Test
    @DisplayName("Should get adapter status")
    void shouldGetStatus() {
        Map<String, Object> status = adapter.getStatus();
        assertThat(status).containsEntry("circuitBreakerName", "audit-database");
    }

    @Test
    @DisplayName("Should execute fallback method directly via reflection to cover it")
    void shouldExecuteFallback() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("aggregateId", "agg-123");

        Exception exception = new RuntimeException("DB Connection Refused");

        // Use reflection to invoke the private fallback method directly
        ReflectionTestUtils.invokeMethod(adapter, "persistEventFallback", event, "CustomerCreated", exception);

        verify(kafkaTemplate).send(eq("audit-events-pending"), eq("agg-123"), mapCaptor.capture());
        
        Map<String, Object> pendingEvent = mapCaptor.getValue();
        assertThat(pendingEvent).containsEntry("eventType", "CustomerCreated");
        assertThat(pendingEvent).containsEntry("originalEventId", "evt-123");
        assertThat(pendingEvent).containsEntry("retryCount", 0);
        assertThat(pendingEvent).containsEntry("reason", "Audit database unavailable");
    }

    @Test
    @DisplayName("Should handle error gracefully inside fallback method")
    void shouldHandleErrorInsideFallback() throws Exception {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-123");
        event.put("aggregateId", "agg-123");

        Exception exception = new RuntimeException("DB Connection Refused");

        when(kafkaTemplate.send(any(), any(), any())).thenThrow(new RuntimeException("Kafka is down too"));

        // Use reflection to invoke the private fallback method directly
        ReflectionTestUtils.invokeMethod(adapter, "persistEventFallback", event, "CustomerCreated", exception);

        // Verify it was called, error caught and didn't crash
        verify(kafkaTemplate).send(any(), any(), any());
    }
}
