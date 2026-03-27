--liquibase formatted sql
--changeset repo:2036-03-27-hypothesis-framework-generation-job dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'hypothesis_framework_generation_job';
CREATE TABLE hypothesis_framework_generation_job (
    id BINARY(16) NOT NULL,
    hypothesis_id BINARY(16) NOT NULL,
    section VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    model VARCHAR(191) NULL,
    worker_id VARCHAR(191) NULL,
    custom_instructions LONGTEXT NULL,
    prompt LONGTEXT NULL,
    request_body_json LONGTEXT NULL,
    raw_response LONGTEXT NULL,
    response_content LONGTEXT NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    cost_usd DECIMAL(10,4) NULL,
    error_message LONGTEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hypothesis_framework_job_status_created (status, created_at),
    KEY idx_hypothesis_framework_job_hypothesis (hypothesis_id),
    CONSTRAINT fk_hypothesis_framework_job_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis (id)
);
