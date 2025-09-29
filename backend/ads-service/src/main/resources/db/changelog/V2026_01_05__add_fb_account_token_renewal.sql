--liquibase formatted sql
--changeset marketinghub:2026-01-05-add-fb-account-token-renewal dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'app_id';
ALTER TABLE fb_account
  ADD COLUMN app_id VARCHAR(255) NULL,
  ADD COLUMN app_secret LONGTEXT NULL,
  ADD COLUMN token_renewal_enabled TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN token_renewal_status VARCHAR(40) NULL,
  ADD COLUMN token_renewal_last_attempt_at DATETIME NULL,
  ADD COLUMN token_renewed_at DATETIME NULL,
  ADD COLUMN token_renewal_last_error LONGTEXT NULL;
