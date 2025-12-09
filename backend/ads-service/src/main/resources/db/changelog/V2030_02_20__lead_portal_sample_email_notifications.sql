--liquibase formatted sql
--changeset repo:2030-02-20-lead-portal-sample-email-notifications dbms:mysql
-- Adds support for selecting a sample email for each experiment and tracking
-- notification dispatch of watermarked image packages. The statements below are
-- idempotent so that deployments where columns were added manually do not fail.

-- Add foreign key on experiment linking to the chosen sample email (only if missing).
SET @experiment_sample_email_col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'experiment'
      AND COLUMN_NAME = 'selected_sample_email_id'
);

SET @ddl := IF(
    @experiment_sample_email_col_exists = 0,
    'ALTER TABLE experiment ADD COLUMN selected_sample_email_id BIGINT NULL',
    'SELECT 1'
);
PREPARE add_experiment_selected_sample_email FROM @ddl;
EXECUTE add_experiment_selected_sample_email;
DEALLOCATE PREPARE add_experiment_selected_sample_email;

SET @experiment_sample_email_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'experiment'
      AND CONSTRAINT_NAME = 'fk_experiment_selected_sample_email'
);

SET @ddl := IF(
    @experiment_sample_email_fk_exists = 0,
    'ALTER TABLE experiment ADD CONSTRAINT fk_experiment_selected_sample_email FOREIGN KEY (selected_sample_email_id) REFERENCES experiment_sample_email (id)',
    'SELECT 1'
);
PREPARE add_experiment_selected_sample_email_fk FROM @ddl;
EXECUTE add_experiment_selected_sample_email_fk;
DEALLOCATE PREPARE add_experiment_selected_sample_email_fk;

-- Track notification status for lead portal image packages (add columns if needed).
SET @notified_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'flow_submission_image_package'
      AND COLUMN_NAME = 'notified_at'
);

SET @ddl := IF(
    @notified_at_exists = 0,
    'ALTER TABLE flow_submission_image_package ADD COLUMN notified_at TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE add_flow_package_notified_at FROM @ddl;
EXECUTE add_flow_package_notified_at;
DEALLOCATE PREPARE add_flow_package_notified_at;

SET @notification_attempts_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'flow_submission_image_package'
      AND COLUMN_NAME = 'notification_attempts'
);

SET @ddl := IF(
    @notification_attempts_exists = 0,
    'ALTER TABLE flow_submission_image_package ADD COLUMN notification_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE add_flow_package_notification_attempts FROM @ddl;
EXECUTE add_flow_package_notification_attempts;
DEALLOCATE PREPARE add_flow_package_notification_attempts;

SET @notification_last_attempt_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'flow_submission_image_package'
      AND COLUMN_NAME = 'notification_last_attempt'
);

SET @ddl := IF(
    @notification_last_attempt_exists = 0,
    'ALTER TABLE flow_submission_image_package ADD COLUMN notification_last_attempt TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE add_flow_package_notification_last_attempt FROM @ddl;
EXECUTE add_flow_package_notification_last_attempt;
DEALLOCATE PREPARE add_flow_package_notification_last_attempt;

SET @notification_last_error_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'flow_submission_image_package'
      AND COLUMN_NAME = 'notification_last_error'
);

SET @ddl := IF(
    @notification_last_error_exists = 0,
    'ALTER TABLE flow_submission_image_package ADD COLUMN notification_last_error TEXT NULL',
    'SELECT 1'
);
PREPARE add_flow_package_notification_last_error FROM @ddl;
EXECUTE add_flow_package_notification_last_error;
DEALLOCATE PREPARE add_flow_package_notification_last_error;
