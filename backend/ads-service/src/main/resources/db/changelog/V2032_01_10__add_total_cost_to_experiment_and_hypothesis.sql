--liquibase formatted sql
--changeset repo:2032-01-10-add-total-cost-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'total_cost';
ALTER TABLE experiment ADD COLUMN total_cost DECIMAL(12,2);

--changeset repo:2032-01-10-add-total-cost-to-hypothesis dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND COLUMN_NAME = 'total_cost';
ALTER TABLE hypothesis ADD COLUMN total_cost DECIMAL(12,2);
