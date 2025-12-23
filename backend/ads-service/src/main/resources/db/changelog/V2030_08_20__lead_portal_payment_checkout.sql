--liquibase formatted sql
--changeset repo:2030-08-20-lead-portal-payment-checkout dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND COLUMN_NAME = 'payment_purchase_id'
ALTER TABLE flow_submission_image_package
    ADD COLUMN payment_purchase_id BIGINT NULL AFTER zip_object_key,
    ADD COLUMN payment_checkout_url VARCHAR(1200) NULL AFTER payment_purchase_id,
    ADD COLUMN payment_checkout_expires_at TIMESTAMP NULL AFTER payment_checkout_url,
    ADD COLUMN payment_amount DECIMAL(12,2) NULL AFTER payment_checkout_expires_at,
    ADD COLUMN payment_currency VARCHAR(12) NULL AFTER payment_amount,
    ADD COLUMN payment_statement_descriptor VARCHAR(120) NULL AFTER payment_currency;

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_submission_image_package' AND INDEX_NAME = 'idx_flow_image_package_payment'
CREATE INDEX idx_flow_image_package_payment
    ON flow_submission_image_package(payment_purchase_id, status);
