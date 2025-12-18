--liquibase formatted sql

--changeset repo:2030-05-20-lead-portal-email-engagement dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND COLUMN_NAME = 'email_opened_at'
ALTER TABLE flow_submission_image_package
    ADD COLUMN email_opened_at TIMESTAMP NULL AFTER notification_last_error,
    ADD COLUMN images_viewed_at TIMESTAMP NULL AFTER email_opened_at;
