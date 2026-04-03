--liquibase formatted sql
--changeset repo:2037-04-03-asset-prompt-intermediate dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'prompt_intermediate';
ALTER TABLE asset
    ADD COLUMN prompt_intermediate LONGTEXT NULL;
