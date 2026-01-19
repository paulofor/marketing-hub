--liquibase formatted sql
--changeset repo:2031-11-15-widen-hypothesis-problem dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'problem' AND DATA_TYPE NOT IN ('text', 'longtext');
ALTER TABLE hypothesis MODIFY COLUMN problem LONGTEXT NULL;
