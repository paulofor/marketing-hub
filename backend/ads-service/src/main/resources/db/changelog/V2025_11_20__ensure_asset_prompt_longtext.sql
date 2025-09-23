--liquibase formatted sql
--changeset marketinghub:2025-11-20-ensure-asset-prompt-longtext dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'prompt'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'prompt' AND data_type = 'longtext'
ALTER TABLE asset MODIFY COLUMN prompt LONGTEXT;
