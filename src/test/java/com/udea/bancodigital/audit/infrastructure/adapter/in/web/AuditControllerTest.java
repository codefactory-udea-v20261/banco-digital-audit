package com.udea.bancodigital.audit.infrastructure.adapter.in.web;

import com.udea.bancodigital.audit.infrastructure.entity.AuditEventEntity;
import com.udea.bancodigital.audit.infrastructure.repository.AuditEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @InjectMocks
    private AuditController auditController;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("Should return event by ID when found")
    void shouldReturnEventById() {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setEventId("evt-123");
        when(auditEventRepository.findByEventId("evt-123")).thenReturn(Optional.of(entity));

        ResponseEntity<AuditEventEntity> response = auditController.getByEventId("evt-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEventId()).isEqualTo("evt-123");
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when event not found")
    void shouldThrowWhenEventNotFound() {
        when(auditEventRepository.findByEventId("evt-404")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> auditController.getByEventId("evt-404"));
    }

    @Test
    @DisplayName("Should query events by event type")
    void shouldQueryByEventType() {
        Page<AuditEventEntity> page = new PageImpl<>(List.of(new AuditEventEntity()));
        when(auditEventRepository.findByEventType(eq("CustomerCreated"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<AuditEventEntity>> response = auditController.queryEvents(
                "CustomerCreated", null, null, null, null, mock(Pageable.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should query events by aggregate ID")
    void shouldQueryByAggregateId() {
        Page<AuditEventEntity> page = new PageImpl<>(List.of(new AuditEventEntity()));
        when(auditEventRepository.findByAggregateId(eq("agg-1"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<AuditEventEntity>> response = auditController.queryEvents(
                null, "agg-1", null, null, null, mock(Pageable.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should query events by user ID")
    void shouldQueryByUserId() {
        Page<AuditEventEntity> page = new PageImpl<>(List.of(new AuditEventEntity()));
        when(auditEventRepository.findByUserId(eq("user-1"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<AuditEventEntity>> response = auditController.queryEvents(
                null, null, "user-1", null, null, mock(Pageable.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should query events by date range")
    void shouldQueryByDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Page<AuditEventEntity> page = new PageImpl<>(List.of(new AuditEventEntity()));
        when(auditEventRepository.findByOccurredAtBetween(eq(start), eq(end), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<AuditEventEntity>> response = auditController.queryEvents(
                null, null, null, start, end, mock(Pageable.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should query all events when no filters provided")
    void shouldQueryAll() {
        Page<AuditEventEntity> page = new PageImpl<>(List.of(new AuditEventEntity()));
        when(auditEventRepository.findAll(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<AuditEventEntity>> response = auditController.queryEvents(
                null, null, null, null, null, mock(Pageable.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
