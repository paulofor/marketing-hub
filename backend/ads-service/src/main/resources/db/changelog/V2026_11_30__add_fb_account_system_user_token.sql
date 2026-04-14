--liquibase formatted sql
--changeset repo:2026-11-30-add-fb-account-system-user-token dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'system_user_access_token';
ALTER TABLE fb_account
    ADD COLUMN system_user_access_token LONGTEXT AFTER app_secret;
