--liquibase formatted sql
--changeset marketinghub:2026-02-15-add-fb-account-business-manager-app-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'business_manager_app_id';
ALTER TABLE fb_account
  ADD COLUMN business_manager_app_id VARCHAR(255) NULL;
