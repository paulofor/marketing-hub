--liquibase formatted sql
--changeset marketinghub:2026-01-17-ensure-hypothesis-mechanism-longtext dbms:mysql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'mechanism' AND DATA_TYPE <> 'longtext';
ALTER TABLE hypothesis MODIFY COLUMN mechanism LONGTEXT;
