--liquibase formatted sql
--changeset repo:2027-02-23-link-fb-adset-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'facebook_ads_ad_set' AND column_name = 'experiment_ad_set_id';
ALTER TABLE facebook_ads_ad_set ADD COLUMN experiment_ad_set_id BIGINT AFTER external_id;
ALTER TABLE facebook_ads_ad_set ADD CONSTRAINT fk_fb_adset_experiment_adset FOREIGN KEY (experiment_ad_set_id) REFERENCES ad_set(id) ON DELETE SET NULL ON UPDATE RESTRICT;
