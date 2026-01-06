--liquibase formatted sql
--changeset marketinghub:2030-10-20-hypothesis-model-and-cost dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND COLUMN_NAME = 'hypothesis_model'
ALTER TABLE market_niche ADD COLUMN hypothesis_model VARCHAR(191);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'cost_usd'
ALTER TABLE hypothesis ADD COLUMN cost_usd DECIMAL(10,4);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'model'
ALTER TABLE hypothesis ADD COLUMN model VARCHAR(191);
