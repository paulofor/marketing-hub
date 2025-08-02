CREATE TABLE lead (
    id BINARY(16) PRIMARY KEY,
    leadgen_id BIGINT UNIQUE,
    instagram_user_id BIGINT,
    ad_id BIGINT,
    campaign_id BIGINT,
    experiment_id BIGINT,
    captured_at TIMESTAMP,
    nurture_stage ENUM('NEW','WARM','HOT') DEFAULT 'NEW',
    cpl DECIMAL(10,2)
);

CREATE INDEX idx_lead_captured_experiment ON lead (captured_at, experiment_id);

CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id BINARY(16),
    event_type VARCHAR(50),
    payload JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL
);
