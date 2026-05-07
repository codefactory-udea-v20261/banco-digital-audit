-- V1__create_audit_table.sql
-- Creates the audit event table for storing domain event audit trails
-- Author: Banco Digital Team

CREATE TABLE IF NOT EXISTS audit_event (
    id              VARCHAR(255) PRIMARY KEY,
    event_id        VARCHAR(255) NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(255),
    correlation_id  VARCHAR(255),
    user_id         VARCHAR(255),
    source_service  VARCHAR(100),
    occurred_at     TIMESTAMP NOT NULL,
    payload         TEXT,
    previous_hash   VARCHAR(64),
    current_hash    VARCHAR(64) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_event(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_aggregate_id ON audit_event(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit_event(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_event(user_id);

COMMENT ON TABLE audit_event IS 'Stores audit trail of all domain events for regulatory compliance';
COMMENT ON COLUMN audit_event.event_id IS 'Unique identifier for the domain event';
COMMENT ON COLUMN audit_event.event_type IS 'Type of event (e.g., CustomerCreated, TransactionCompleted)';
COMMENT ON COLUMN audit_event.aggregate_id IS 'The aggregate ID this event relates to';
COMMENT ON COLUMN audit_event.payload IS 'Full event payload as JSON for complete audit trail';
