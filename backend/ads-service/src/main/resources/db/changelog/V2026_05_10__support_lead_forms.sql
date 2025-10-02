--liquibase formatted sql
--changeset repo:2026-05-10-add-creative-lead-form dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'creative' AND column_name = 'lead_gen_form_id';
ALTER TABLE creative
    ADD COLUMN lead_gen_form_id VARCHAR(64) AFTER destination_url;

--changeset repo:2026-05-10-add-account-default-lead-form dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'default_lead_gen_form_id';
ALTER TABLE fb_account
    ADD COLUMN default_lead_gen_form_id VARCHAR(64) AFTER default_website_url;
