--liquibase formatted sql
--changeset repo:2030-03-05-lead-portal-zip-archives dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND COLUMN_NAME = 'zip_object_key'
ALTER TABLE flow_submission_image_package
    ADD COLUMN zip_object_key VARCHAR(512) NULL,
    ADD COLUMN zip_size_bytes BIGINT NULL,
    ADD COLUMN zip_generated_at TIMESTAMP NULL,
    ADD COLUMN zip_last_error TEXT NULL,
    ADD COLUMN zip_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN zip_last_attempt TIMESTAMP NULL;

--changeset repo:2030-03-05-lead-portal-zip-index dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND INDEX_NAME = 'idx_flow_image_package_zip_status'
CREATE INDEX idx_flow_image_package_zip_status
    ON flow_submission_image_package(zip_object_key, status, updated_at);
