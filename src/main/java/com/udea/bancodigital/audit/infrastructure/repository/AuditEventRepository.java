package com.udea.bancodigital.audit.infrastructure.repository;

import com.udea.bancodigital.audit.infrastructure.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    Optional<AuditEventEntity> findByEventId(String eventId);

    Page<AuditEventEntity> findByEventType(String eventType, Pageable pageable);

    Page<AuditEventEntity> findByAggregateId(String aggregateId, Pageable pageable);

    Page<AuditEventEntity> findByUserId(String userId, Pageable pageable);

    Page<AuditEventEntity> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    boolean existsByEventId(String eventId);
}
