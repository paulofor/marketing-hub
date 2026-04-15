--liquibase formatted sql
--changeset repo:2026-04-15-add-fb-account-pixel-owner-business-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'pixel_owner_business_id';
ALTER TABLE fb_account
  ADD COLUMN pixel_owner_business_id VARCHAR(64) NULL;
