--liquibase formatted sql
--changeset repo:2030-02-20-lead-portal-sample-email-notifications dbms:mysql
-- Adds support for selecting a sample email for each experiment and tracking
-- notification dispatch of watermarked image packages.

-- Add foreign key on experiment linking to the chosen sample email.
ALTER TABLE experiment
    ADD COLUMN selected_sample_email_id BIGINT NULL,
    ADD CONSTRAINT fk_experiment_selected_sample_email
        FOREIGN KEY (selected_sample_email_id)
            REFERENCES experiment_sample_email (id);

-- Track notification status for lead portal image packages.
ALTER TABLE flow_submission_image_package
    ADD COLUMN notified_at TIMESTAMP NULL,
    ADD COLUMN notification_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN notification_last_attempt TIMESTAMP NULL,
    ADD COLUMN notification_last_error TEXT NULL;
