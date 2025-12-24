--liquibase formatted sql
--changeset repo:2030-08-30-experiment-unit-price-brl dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'unit_price_brl'
ALTER TABLE experiment
    ADD COLUMN unit_price_brl DECIMAL(10,2) NULL AFTER daily_budget;
