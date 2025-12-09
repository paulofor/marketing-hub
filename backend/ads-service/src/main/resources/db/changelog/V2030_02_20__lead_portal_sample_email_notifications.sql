--liquibase formatted sql

--changeset repo:2030-02-20-add-selected-sample-email-column dbms:mysql
-- Adds support for selecting a sample email for each experiment.
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'selected_sample_email_id'
ALTER TABLE experiment
    ADD COLUMN selected_sample_email_id BIGINT NULL;

--changeset repo:2030-02-20-add-selected-sample-email-fk dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND CONSTRAINT_NAME = 'fk_experiment_selected_sample_email'
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'selected_sample_email_id'
ALTER TABLE experiment
    ADD CONSTRAINT fk_experiment_selected_sample_email
        FOREIGN KEY (selected_sample_email_id)
            REFERENCES experiment_sample_email (id);

--changeset repo:2030-02-20-lead-portal-sample-email-notifications dbms:mysql
-- Tracks notification dispatch of watermarked image packages.
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND COLUMN_NAME IN ('notified_at','notification_attempts','notification_last_attempt','notification_last_error')
ALTER TABLE flow_submission_image_package
    ADD COLUMN notified_at TIMESTAMP NULL,
    ADD COLUMN notification_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN notification_last_attempt TIMESTAMP NULL,
    ADD COLUMN notification_last_error TEXT NULL;
