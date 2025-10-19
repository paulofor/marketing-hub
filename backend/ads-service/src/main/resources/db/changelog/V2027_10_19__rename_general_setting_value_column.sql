--liquibase formatted sql
--changeset marketinghub:2027-10-19-rename-general-setting-value-column dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'general_setting' AND column_name = 'value';
ALTER TABLE general_setting CHANGE COLUMN value setting_value LONGTEXT;
