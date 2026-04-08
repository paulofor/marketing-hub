--liquibase formatted sql
--changeset repo:2037-04-08-framework-image-generation-job-queue dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'framework_image_generation_job';
CREATE TABLE framework_image_generation_job (
    id CHAR(36) NOT NULL,
    experiment_id BIGINT NOT NULL,
    planning_item_key VARCHAR(191) NOT NULL,
    status VARCHAR(32) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    worker_id VARCHAR(191) NULL,
    model VARCHAR(191) NULL,
    prompt LONGTEXT NULL,
    batch_id VARCHAR(191) NULL,
    asset_id BIGINT NULL,
    source_url VARCHAR(1024) NULL,
    web_url VARCHAR(1024) NULL,
    error_message LONGTEXT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_framework_image_job_experiment FOREIGN KEY (experiment_id) REFERENCES experiment (id)
);

CREATE INDEX idx_framework_image_job_pending ON framework_image_generation_job (status, created_at);
CREATE INDEX idx_framework_image_job_experiment_item ON framework_image_generation_job (experiment_id, planning_item_key, status, created_at);
