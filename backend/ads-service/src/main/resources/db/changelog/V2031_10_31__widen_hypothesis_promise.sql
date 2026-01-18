--liquibase formatted sql
--changeset marketinghub:2031-10-31-widen-hypothesis-promise dbms:mysql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'promise' AND DATA_TYPE NOT IN ('text', 'longtext');
ALTER TABLE hypothesis MODIFY COLUMN promise LONGTEXT NULL;
