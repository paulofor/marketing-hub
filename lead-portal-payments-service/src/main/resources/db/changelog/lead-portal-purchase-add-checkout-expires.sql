--liquibase formatted sql
--changeset repo:2024-06-05-add-checkout-expires dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_purchase' AND column_name = 'checkout_expires_at';

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'lead_portal_purchase'
    AND column_name = 'checkout_expires_at'
);

SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE lead_portal_purchase ADD COLUMN checkout_expires_at TIMESTAMP NULL AFTER checkout_url;',
  'SELECT 0;'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
