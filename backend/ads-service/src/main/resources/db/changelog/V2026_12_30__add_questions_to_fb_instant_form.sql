--liquibase formatted sql
--changeset repo:2026-12-30-add-questions-to-fb-instant-form dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'fb_instant_form' AND column_name = 'questions';
ALTER TABLE fb_instant_form
    ADD COLUMN questions LONGTEXT NULL AFTER prompt;
