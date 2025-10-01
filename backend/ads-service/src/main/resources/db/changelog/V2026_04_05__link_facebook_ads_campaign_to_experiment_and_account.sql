--liquibase formatted sql
--changeset marketinghub:2026-04-05-link-facebook-ads-campaign-relations dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'facebook_ads_campaign' AND column_name = 'experiment_id';
ALTER TABLE facebook_ads_campaign
  ADD COLUMN experiment_id BIGINT NULL,
  ADD COLUMN facebook_account_id BIGINT NULL;

UPDATE facebook_ads_campaign c
JOIN experiment e ON e.name = c.name
SET c.experiment_id = e.id
WHERE c.experiment_id IS NULL;

UPDATE facebook_ads_campaign c
JOIN fb_account a ON a.ad_account_id = c.ad_account_id
SET c.facebook_account_id = a.id
WHERE c.facebook_account_id IS NULL;

ALTER TABLE facebook_ads_campaign
  MODIFY COLUMN experiment_id BIGINT NOT NULL,
  MODIFY COLUMN facebook_account_id BIGINT NOT NULL,
  ADD CONSTRAINT fk_facebook_ads_campaign_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id),
  ADD CONSTRAINT fk_facebook_ads_campaign_account FOREIGN KEY (facebook_account_id) REFERENCES fb_account(id);
