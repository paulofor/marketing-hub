-- liquibase formatted sql
-- changeset marketinghub:2025-08-01-exp-financial-fields
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'kpi_target_cpl';
ALTER TABLE experiment ADD COLUMN kpi_target_cpl DECIMAL(10,2) DEFAULT 45.00;

--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'stop_loss_cpl';
ALTER TABLE experiment ADD COLUMN stop_loss_cpl DECIMAL(10,2) DEFAULT 90.00;

--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'sample_size';
ALTER TABLE experiment ADD COLUMN sample_size INT DEFAULT 1500;

--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'baseline_cvr';
ALTER TABLE experiment ADD COLUMN baseline_cvr DECIMAL(5,2) DEFAULT 3.00;

--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'target_cvr';
ALTER TABLE experiment ADD COLUMN target_cvr DECIMAL(5,2) DEFAULT 5.00;

--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'mde_percent';
ALTER TABLE experiment ADD COLUMN mde_percent DECIMAL(5,2) DEFAULT 40.0;

--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND CONSTRAINT_NAME = 'chk_exp_stoploss' AND CONSTRAINT_TYPE = 'CHECK';
ALTER TABLE experiment ADD CONSTRAINT chk_exp_stoploss CHECK (stop_loss_cpl >= kpi_target_cpl);
