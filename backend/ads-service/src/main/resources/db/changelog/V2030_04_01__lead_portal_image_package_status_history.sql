--liquibase formatted sql

--changeset repo:2030-04-01-create-flow-submission-image-package-status-history dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_package_status_history'
CREATE TABLE flow_submission_image_package_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_package_status_history_package FOREIGN KEY (package_id) REFERENCES flow_submission_image_package(id) ON DELETE CASCADE
);

--changeset repo:2030-04-01-create-flow-submission-image-package-status-history-index dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_package_status_history' AND index_name = 'idx_flow_submission_image_package_status_history_package'
CREATE INDEX idx_flow_submission_image_package_status_history_package
    ON flow_submission_image_package_status_history(package_id, created_at, id);

--changeset repo:2030-04-01-backfill-flow-submission-image-package-status-history dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM flow_submission_image_package_status_history
INSERT INTO flow_submission_image_package_status_history (package_id, status, failure_reason, created_at)
SELECT id, status, failure_reason, COALESCE(updated_at, created_at)
FROM flow_submission_image_package;
