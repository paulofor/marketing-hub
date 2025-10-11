--liquibase formatted sql
--changeset repo:2026-09-12-add-worker-requests-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'instant_forms_to_generate';
ALTER TABLE experiment
    ADD COLUMN instant_forms_to_generate INT NULL AFTER creatives_to_generate,
    ADD COLUMN emails_to_generate INT NULL AFTER instant_forms_to_generate;
