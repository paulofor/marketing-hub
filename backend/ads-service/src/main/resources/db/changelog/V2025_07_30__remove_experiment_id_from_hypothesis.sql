-- liquibase formatted sql
-- changeset marketinghub:2025-07-30-drop-experiment-id
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'experiment_id';
ALTER TABLE hypothesis DROP COLUMN experiment_id;
