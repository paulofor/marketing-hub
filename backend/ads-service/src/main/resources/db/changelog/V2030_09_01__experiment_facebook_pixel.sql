--liquibase formatted sql
--changeset marketinghub:2030-09-01-experiment-facebook-pixel dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'facebook_pixel_id'
ALTER TABLE experiment
    ADD COLUMN facebook_pixel_id VARCHAR(64) NULL AFTER facebook_instant_form_id,
    ADD COLUMN facebook_pixel_code LONGTEXT NULL AFTER facebook_pixel_id,
    ADD COLUMN facebook_pixel_created_at TIMESTAMP NULL AFTER facebook_pixel_code;

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_purchase' AND column_name = 'pixel_conversion_recorded_at'
ALTER TABLE lead_portal_purchase
    ADD COLUMN pixel_conversion_recorded_at TIMESTAMP NULL AFTER payment_approved_at;
