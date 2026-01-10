--liquibase formatted sql
--changeset marketinghub:2031-04-01-add-market-niche-cost dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND COLUMN_NAME = 'cost';
ALTER TABLE market_niche ADD COLUMN cost DECIMAL(10,2);

--changeset marketinghub:2031-04-01-add-market-niche-expense dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_niche' AND COLUMN_NAME = 'expense';
ALTER TABLE market_niche ADD COLUMN expense DECIMAL(10,2);

--changeset marketinghub:2031-04-01-add-hypothesis-cost dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'cost';
ALTER TABLE hypothesis ADD COLUMN cost DECIMAL(10,2);

--changeset marketinghub:2031-04-01-add-hypothesis-expense dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'expense';
ALTER TABLE hypothesis ADD COLUMN expense DECIMAL(10,2);

--changeset marketinghub:2031-04-01-add-experiment-cost dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'cost';
ALTER TABLE experiment ADD COLUMN cost DECIMAL(10,2);

--changeset marketinghub:2031-04-01-add-experiment-expense dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'expense';
ALTER TABLE experiment ADD COLUMN expense DECIMAL(10,2);
