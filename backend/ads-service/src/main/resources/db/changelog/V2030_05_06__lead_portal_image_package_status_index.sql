--liquibase formatted sql
--changeset repo:2030-05-06-lead-portal-image-package-status-index dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND INDEX_NAME = 'idx_flow_image_package_status_created_at'
CREATE INDEX idx_flow_image_package_status_created_at
    ON flow_submission_image_package(status, created_at);
