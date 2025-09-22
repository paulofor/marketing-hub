--liquibase formatted sql

--changeset marketinghub:2025-11-05-widen-asset-provider dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'provider' AND data_type = 'varchar' AND character_maximum_length >= 255
ALTER TABLE asset MODIFY COLUMN provider VARCHAR(255);
