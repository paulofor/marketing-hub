--liquibase formatted sql
--changeset repo:2028-05-10-rename-lead-portal-option-column dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question_option' AND column_name = 'value'
ALTER TABLE lead_portal_flow_question_option CHANGE COLUMN value option_value VARCHAR(255) NOT NULL;
