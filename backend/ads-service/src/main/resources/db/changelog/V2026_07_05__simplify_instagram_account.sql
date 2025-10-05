--liquibase formatted sql
--changeset marketinghub:2026-07-05-simplify-instagram-account dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ig_account' AND column_name = 'handle';
ALTER TABLE ig_account
  ADD COLUMN handle VARCHAR(255) NULL,
  ADD COLUMN account_code VARCHAR(255) NULL;

UPDATE ig_account
SET handle = CASE
    WHEN name IS NULL OR TRIM(name) = '' THEN CONCAT('@conta', id)
    WHEN LOCATE('@', name) = 1 THEN name
    ELSE CONCAT('@', REPLACE(LOWER(name), ' ', ''))
  END
WHERE handle IS NULL OR TRIM(handle) = '';

UPDATE ig_account
SET account_code = COALESCE(account_code, instagram_user_id, CONCAT('IG-', id))
WHERE account_code IS NULL OR TRIM(account_code) = '';

ALTER TABLE ig_account
  MODIFY COLUMN handle VARCHAR(255) NOT NULL,
  MODIFY COLUMN account_code VARCHAR(255) NOT NULL;

ALTER TABLE ig_account
  DROP COLUMN currency,
  DROP COLUMN avatar_url,
  DROP COLUMN instagram_user_id,
  DROP COLUMN facebook_page_id,
  DROP COLUMN ad_account_id,
  DROP COLUMN access_token;
