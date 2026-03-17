--liquibase formatted sql
--changeset repo:2030-12-20-lead-portal-image-prompt-template dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'image_prompt_model'
ALTER TABLE lead_portal_flow
    ADD COLUMN image_prompt_model VARCHAR(128) NULL AFTER model,
    ADD COLUMN image_prompt_template LONGTEXT NULL AFTER image_prompt_model,
    ADD COLUMN image_prompt_batch_size INT NULL AFTER image_prompt_template;
