--liquibase formatted sql
--changeset marketinghub:2031-04-15-add-market-niche-total-cost dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND COLUMN_NAME = 'total_cost';
ALTER TABLE market_niche ADD COLUMN total_cost DECIMAL(12,2);

--changeset marketinghub:2031-04-15-add-market-niche-total-revenue dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND COLUMN_NAME = 'total_revenue';
ALTER TABLE market_niche ADD COLUMN total_revenue DECIMAL(12,2);
