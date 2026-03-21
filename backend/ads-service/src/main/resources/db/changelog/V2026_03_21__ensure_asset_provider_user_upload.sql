--liquibase formatted sql

--changeset marketinghub:2026-03-21-ensure-asset-provider-user-upload dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'provider' AND (data_type = 'enum' OR (data_type = 'varchar' AND character_maximum_length < 64))
ALTER TABLE asset MODIFY COLUMN provider VARCHAR(64);
