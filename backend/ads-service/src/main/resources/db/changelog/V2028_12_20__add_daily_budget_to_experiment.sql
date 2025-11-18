--liquibase formatted sql
--changeset repo:2028-12-20-add-daily-budget-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'daily_budget';

ALTER TABLE experiment ADD COLUMN daily_budget DECIMAL(10, 2);
