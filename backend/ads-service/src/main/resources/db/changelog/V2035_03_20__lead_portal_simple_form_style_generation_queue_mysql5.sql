--liquibase formatted sql
--changeset repo:2035-03-20-lead-portal-simple-form-style-generation-queue dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_simple_form_style' AND column_name = 'generation_status';
ALTER TABLE lead_portal_simple_form_style
    ADD COLUMN generation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' AFTER generation_cost_usd,
    ADD COLUMN generation_error LONGTEXT NULL AFTER generation_status;

UPDATE lead_portal_simple_form_style
SET generation_status = CASE
    WHEN definition IS NOT NULL THEN 'COMPLETED'
    ELSE 'PENDING'
END,
    generation_error = NULL
WHERE generation_status IS NULL OR generation_status = '';
