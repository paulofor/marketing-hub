--liquibase formatted sql
--changeset repo:2026-10-15-create-ai-worker-generation dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'ai_worker_generation';
CREATE TABLE ai_worker_generation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100) NULL,
    model VARCHAR(191) NULL,
    prompt LONGTEXT NULL,
    raw_response LONGTEXT NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    cost_usd DECIMAL(10,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_ai_worker_generation_domain_created_at
    ON ai_worker_generation (domain, created_at DESC);
