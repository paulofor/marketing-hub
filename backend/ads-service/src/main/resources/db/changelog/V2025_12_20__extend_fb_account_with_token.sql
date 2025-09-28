--liquibase formatted sql
--changeset marketinghub:2025-12-20-extend-fb-account-with-token dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'access_token';
ALTER TABLE fb_account
  ADD COLUMN access_token LONGTEXT NULL,
  ADD COLUMN token_expires_at DATETIME NULL,
  ADD COLUMN token_last_refreshed_at DATETIME NULL,
  ADD COLUMN authorized_user_id VARCHAR(128) NULL,
  ADD COLUMN authorized_user_name VARCHAR(255) NULL,
  ADD COLUMN authorized_user_email VARCHAR(320) NULL;
