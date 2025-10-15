--liquibase formatted sql
--changeset repo:2026-11-26-allow-null-form-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'fb_instant_form' AND column_name = 'form_id' AND IS_NULLABLE = 'YES';
ALTER TABLE fb_instant_form
    MODIFY form_id VARCHAR(128) NULL;
