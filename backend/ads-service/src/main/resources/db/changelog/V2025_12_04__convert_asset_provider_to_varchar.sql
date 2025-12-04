--liquibase formatted sql

--changeset marketinghub:2025-12-04-convert-asset-provider-to-varchar dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'provider' AND data_type = 'enum'
ALTER TABLE asset MODIFY COLUMN provider VARCHAR(255);
