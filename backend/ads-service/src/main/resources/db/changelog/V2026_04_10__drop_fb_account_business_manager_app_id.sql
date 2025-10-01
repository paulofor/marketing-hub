--liquibase formatted sql
--changeset repo:2026-04-10-drop-fb-account-business-manager-app-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'business_manager_app_id';
ALTER TABLE fb_account
  DROP COLUMN business_manager_app_id;
