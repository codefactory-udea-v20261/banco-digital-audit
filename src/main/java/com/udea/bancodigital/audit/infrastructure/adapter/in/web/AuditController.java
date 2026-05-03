package com.udea.bancodigital.audit.infrastructure.adapter.in.web;

import com.udea.bancodigital.audit.infrastructure.entity.AuditEventEntity;
import com.udea.bancodigital.audit.infrastructure.repository.AuditEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit event query endpoints")
@PreAuthorize("hasAuthority('PERM_VIEW_AUDIT')")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping("/{eventId}")
    @Operation(summary = "Get audit event by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event found"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<AuditEventEntity> getByEventId(
            @Parameter(description = "Event ID") @PathVariable String eventId) {
        return auditEventRepository.findByEventId(eventId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new NoSuchElementException("Audit event not found: " + eventId));
    }

    @GetMapping
    @Operation(summary = "Query audit events with filters")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Events returned")
    })
    public ResponseEntity<Page<AuditEventEntity>> queryEvents(
            @Parameter(description = "Filter by event type") @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by aggregate ID") @RequestParam(required = false) String aggregateId,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Start date") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AuditEventEntity> results;

        if (eventType != null) {
            results = auditEventRepository.findByEventType(eventType, pageable);
        } else if (aggregateId != null) {
            results = auditEventRepository.findByAggregateId(aggregateId, pageable);
        } else if (userId != null) {
            results = auditEventRepository.findByUserId(userId, pageable);
        } else if (startDate != null && endDate != null) {
            results = auditEventRepository.findByOccurredAtBetween(startDate, endDate, pageable);
        } else {
            results = auditEventRepository.findAll(pageable);
        }

        return ResponseEntity.ok(results);
    }
}
