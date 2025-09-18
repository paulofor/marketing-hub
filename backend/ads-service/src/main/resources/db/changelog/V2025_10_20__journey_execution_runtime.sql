--liquibase formatted sql

--changeset marketinghub:2025-10-20-journey-assignment-execution-runtime
ALTER TABLE journey_assignment
    ADD COLUMN next_attempt_at DATETIME NULL,
    ADD COLUMN retry_count INT DEFAULT 0;

--changeset marketinghub:2025-10-20-journey-assignment-execution-index
CREATE INDEX idx_journey_assignment_next_attempt
    ON journey_assignment(next_attempt_at);
