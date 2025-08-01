-- liquibase formatted sql
-- changeset marketinghub:2025-08-01-exp-financial-fields
ALTER TABLE experiment
    ADD COLUMN kpi_target_cpl DECIMAL(10,2) DEFAULT 45.00,
    ADD COLUMN stop_loss_cpl DECIMAL(10,2) DEFAULT 90.00,
    ADD COLUMN sample_size INT DEFAULT 1500,
    ADD COLUMN baseline_cvr DECIMAL(5,2) DEFAULT 3.00,
    ADD COLUMN target_cvr DECIMAL(5,2) DEFAULT 5.00,
    ADD COLUMN mde_percent DECIMAL(5,2) DEFAULT 40.0;
ALTER TABLE experiment
    ADD CONSTRAINT chk_exp_stoploss CHECK (stop_loss_cpl >= kpi_target_cpl);
