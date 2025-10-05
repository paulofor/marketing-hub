--liquibase formatted sql
--changeset marketinghub:2026-06-01-extend-instagram-account dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ig_account' AND column_name = 'instagram_user_id';
ALTER TABLE ig_account
  ADD COLUMN instagram_user_id VARCHAR(64) NULL,
  ADD COLUMN facebook_page_id VARCHAR(64) NULL,
  ADD COLUMN ad_account_id VARCHAR(64) NULL,
  ADD COLUMN access_token LONGTEXT NULL,
  MODIFY COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'BRL';

UPDATE ig_account
SET currency = 'BRL'
WHERE currency IS NULL OR currency <> 'BRL';
