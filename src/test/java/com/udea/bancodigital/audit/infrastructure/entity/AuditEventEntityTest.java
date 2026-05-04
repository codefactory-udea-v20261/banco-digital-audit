package com.udea.bancodigital.audit.infrastructure.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventEntityTest {

    @Test
    @DisplayName("Should create AuditEventEntity using builder")
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();
        String id = UUID.randomUUID().toString();
        
        AuditEventEntity entity = AuditEventEntity.builder()
                .id(id)
                .eventId("evt-001")
                .eventType("TEST_EVENT")
                .aggregateId("agg-001")
                .correlationId("corr-001")
                .userId("user-001")
                .sourceService("test-service")
                .occurredAt(now)
                .payload("{\"key\":\"value\"}")
                .createdAt(now)
                .build();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEventId()).isEqualTo("evt-001");
        assertThat(entity.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(entity.getAggregateId()).isEqualTo("agg-001");
        assertThat(entity.getCorrelationId()).isEqualTo("corr-001");
        assertThat(entity.getUserId()).isEqualTo("user-001");
        assertThat(entity.getSourceService()).isEqualTo("test-service");
        assertThat(entity.getOccurredAt()).isEqualTo(now);
        assertThat(entity.getPayload()).isEqualTo("{\"key\":\"value\"}");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create AuditEventEntity using setters")
    void testSetters() {
        LocalDateTime now = LocalDateTime.now();
        String id = UUID.randomUUID().toString();
        
        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(id);
        entity.setEventId("evt-002");
        entity.setEventType("TEST_EVENT_2");
        entity.setAggregateId("agg-002");
        entity.setCorrelationId("corr-002");
        entity.setUserId("user-002");
        entity.setSourceService("test-service-2");
        entity.setOccurredAt(now);
        entity.setPayload("{\"key\":\"value2\"}");
        entity.setCreatedAt(now);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEventId()).isEqualTo("evt-002");
        assertThat(entity.getEventType()).isEqualTo("TEST_EVENT_2");
        assertThat(entity.getAggregateId()).isEqualTo("agg-002");
        assertThat(entity.getCorrelationId()).isEqualTo("corr-002");
        assertThat(entity.getUserId()).isEqualTo("user-002");
        assertThat(entity.getSourceService()).isEqualTo("test-service-2");
        assertThat(entity.getOccurredAt()).isEqualTo(now);
        assertThat(entity.getPayload()).isEqualTo("{\"key\":\"value2\"}");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }
}
