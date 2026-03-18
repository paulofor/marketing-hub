--liquibase formatted sql
--changeset repo:2036-12-22-lead-portal-flow-custom-form-html dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'custom_form_html';
ALTER TABLE lead_portal_flow
    ADD COLUMN custom_form_html LONGTEXT NULL;
