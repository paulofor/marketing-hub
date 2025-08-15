-- liquibase formatted sql
-- changeset marketinghub:2025-09-16-add-mechanism-to-hypothesis
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'mechanism';
ALTER TABLE hypothesis ADD COLUMN mechanism LONGTEXT;
