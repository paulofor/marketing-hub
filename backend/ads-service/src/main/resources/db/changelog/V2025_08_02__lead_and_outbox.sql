-- liquibase formatted sql
-- Creates lead and outbox tables

--changeset marketinghub:2025-08-02-create-lead validCheckSum:9:59377eb84c77f99a62db5b10494e8299
CREATE TABLE `lead` (
    id BINARY(16) PRIMARY KEY,
    leadgen_id BIGINT UNIQUE,
    instagram_user_id BIGINT,
    ad_id BIGINT,
    campaign_id BIGINT,
    experiment_id BIGINT,
    captured_at DATETIME DEFAULT NULL,
    nurture_stage ENUM('NEW','WARM','HOT') DEFAULT 'NEW',
    cpl DECIMAL(10,2)
) ENGINE=InnoDB;

--changeset marketinghub:2025-08-02-lead-index validCheckSum:9:d2b6d6ee31ec84b98fd1e17a4305d0e5
CREATE INDEX idx_lead_captured_experiment ON `lead` (captured_at, experiment_id);

--changeset marketinghub:2025-08-02-create-outbox
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id BINARY(16),
    event_type VARCHAR(50),
    payload TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME DEFAULT NULL
) ENGINE=InnoDB;
