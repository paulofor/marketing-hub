--liquibase formatted sql
--changeset marketinghub:2026-03-20-extend-fb-account-worker-config dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'fb_account' AND column_name = 'ad_account_id';
ALTER TABLE fb_account
  ADD COLUMN ad_account_id VARCHAR(64) NULL,
  ADD COLUMN default_page_id VARCHAR(128) NULL,
  ADD COLUMN default_website_url VARCHAR(512) NULL,
  ADD COLUMN default_instagram_actor_id VARCHAR(64) NULL,
  ADD COLUMN default_creative_message_template VARCHAR(255) NULL,
  ADD COLUMN default_call_to_action_type VARCHAR(64) NULL,
  ADD COLUMN ad_set_daily_budget VARCHAR(32) NULL,
  ADD COLUMN ad_set_billing_event VARCHAR(64) NULL,
  ADD COLUMN ad_set_optimization_goal VARCHAR(64) NULL,
  ADD COLUMN ad_set_destination_type VARCHAR(64) NULL,
  ADD COLUMN ad_set_bid_strategy VARCHAR(64) NULL,
  ADD COLUMN ad_set_bid_amount VARCHAR(32) NULL,
  ADD COLUMN ad_set_target_country VARCHAR(32) NULL,
  ADD COLUMN worker_enabled TINYINT(1) NOT NULL DEFAULT 0;
