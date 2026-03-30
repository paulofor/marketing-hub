--liquibase formatted sql
--changeset repo:2037-03-30-experiment-pipeline-generation-job dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'campaign_angle';
ALTER TABLE experiment
    ADD COLUMN campaign_angle LONGTEXT NULL,
    ADD COLUMN ad_copy LONGTEXT NULL,
    ADD COLUMN ad_image_briefing LONGTEXT NULL,
    ADD COLUMN landing_page_copy LONGTEXT NULL,
    ADD COLUMN landing_page_wireframe LONGTEXT NULL;

--changeset repo:2037-03-30-experiment-pipeline-generation-job-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'experiment_pipeline_generation_job';
CREATE TABLE experiment_pipeline_generation_job (
    id CHAR(36) NOT NULL,
    experiment_id BIGINT NOT NULL,
    section VARCHAR(48) NOT NULL,
    status VARCHAR(32) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    model VARCHAR(191) NULL,
    worker_id VARCHAR(191) NULL,
    custom_instructions LONGTEXT NULL,
    prompt LONGTEXT NULL,
    request_body_json LONGTEXT NULL,
    raw_response LONGTEXT NULL,
    response_content LONGTEXT NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    cost_usd DECIMAL(10, 4) NULL,
    error_message LONGTEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_experiment_pipeline_job_status_created (status, created_at),
    KEY idx_experiment_pipeline_job_experiment (experiment_id),
    CONSTRAINT fk_experiment_pipeline_job_experiment FOREIGN KEY (experiment_id) REFERENCES experiment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
