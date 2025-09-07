-- liquibase formatted sql

-- changeset marketinghub:2025-10-05-create-facebook-ads-campaign
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_campaign"/>
--    </not>
CREATE TABLE facebook_ads_campaign (
  id                   CHAR(36)      NOT NULL,
  external_id          VARCHAR(64)   NULL,
  ad_account_id        VARCHAR(64)   NOT NULL,
  name                 VARCHAR(255)  NOT NULL,
  objective            VARCHAR(64)   NOT NULL,
  status               ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  budget_mode          ENUM('CAMPAIGN','ADSET') NOT NULL,
  daily_budget_minor   BIGINT UNSIGNED NULL,
  lifetime_budget_minor BIGINT UNSIGNED NULL,
  api_version          VARCHAR(16)   NULL,
  created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-campaign-special-ad-category
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_campaign_special_ad_category"/>
--    </not>
CREATE TABLE facebook_ads_campaign_special_ad_category (
  campaign_id   CHAR(36)    NOT NULL,
  category      ENUM('NONE','CREDIT','EMPLOYMENT','HOUSING','ISSUES_ELECTIONS_POLITICS') NOT NULL,
  PRIMARY KEY (campaign_id, category),
  CONSTRAINT fk_facebook_ads_csac_campaign
    FOREIGN KEY (campaign_id) REFERENCES facebook_ads_campaign(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-campaign-special-ad-country
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_campaign_special_ad_country"/>
--    </not>
CREATE TABLE facebook_ads_campaign_special_ad_country (
  campaign_id   CHAR(36) NOT NULL,
  country_iso2  CHAR(2)  NOT NULL,
  PRIMARY KEY (campaign_id, country_iso2),
  CONSTRAINT fk_facebook_ads_csacountry_campaign
    FOREIGN KEY (campaign_id) REFERENCES facebook_ads_campaign(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-ad-set
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_ad_set"/>
--    </not>
CREATE TABLE facebook_ads_ad_set (
  id                       CHAR(36)    NOT NULL,
  external_id              VARCHAR(64) NULL,
  campaign_id              CHAR(36)    NOT NULL,
  name                     VARCHAR(255) NOT NULL,
  status                   ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  daily_budget_minor       BIGINT UNSIGNED NULL,
  lifetime_budget_minor    BIGINT UNSIGNED NULL,
  start_time               DATETIME   NULL,
  end_time                 DATETIME   NULL,
  billing_event            VARCHAR(32) NOT NULL,
  optimization_goal        VARCHAR(64) NOT NULL,
  bid_strategy             VARCHAR(64) NOT NULL,
  bid_amount_minor         BIGINT UNSIGNED NULL,
  promoted_object_json     LONGTEXT   NULL,
  targeting_json           LONGTEXT   NOT NULL,
  created_at               TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_facebook_ads_adset_campaign
    FOREIGN KEY (campaign_id) REFERENCES facebook_ads_campaign(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-media-asset
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_media_asset"/>
--    </not>
CREATE TABLE facebook_ads_media_asset (
  id            CHAR(36)     NOT NULL,
  kind          ENUM('IMAGE','VIDEO') NOT NULL,
  source_uri    VARCHAR(1024) NULL,
  image_hash    VARCHAR(128)  NULL,
  video_id      VARCHAR(64)   NULL,
  width         INT UNSIGNED  NULL,
  height        INT UNSIGNED  NULL,
  duration_ms   INT UNSIGNED  NULL,
  checksum      VARCHAR(128)  NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-ad-creative
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_ad_creative"/>
--    </not>
CREATE TABLE facebook_ads_ad_creative (
  id                  CHAR(36)    NOT NULL,
  external_id         VARCHAR(64) NULL,
  page_id             VARCHAR(64) NOT NULL,
  instagram_user_id   VARCHAR(64) NULL,
  kind                ENUM('LINK','VIDEO','CAROUSEL') NOT NULL,
  link_data_json      LONGTEXT NULL,
  video_data_json     LONGTEXT NULL,
  carousel_data_json  LONGTEXT NULL,
  last_preview_url    VARCHAR(1024) NULL,
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-ad
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_ad"/>
--    </not>
CREATE TABLE facebook_ads_ad (
  id            CHAR(36)   NOT NULL,
  external_id   VARCHAR(64) NULL,
  adset_id      CHAR(36)   NOT NULL,
  name          VARCHAR(255) NOT NULL,
  creative_id   CHAR(36)   NOT NULL,
  status        ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_facebook_ads_ad_adset
    FOREIGN KEY (adset_id) REFERENCES facebook_ads_ad_set(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_facebook_ads_ad_creative
    FOREIGN KEY (creative_id) REFERENCES facebook_ads_ad_creative(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- changeset marketinghub:2025-10-05-create-facebook-ads-ad-tracking-utm
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="facebook_ads_ad_tracking_utm"/>
--    </not>
CREATE TABLE facebook_ads_ad_tracking_utm (
  ad_id        CHAR(36)    NOT NULL,
  utm_source   VARCHAR(64)  NULL,
  utm_medium   VARCHAR(64)  NULL,
  utm_campaign VARCHAR(128) NULL,
  utm_content  VARCHAR(128) NULL,
  utm_term     VARCHAR(128) NULL,
  PRIMARY KEY (ad_id),
  CONSTRAINT fk_facebook_ads_utm_ad
    FOREIGN KEY (ad_id) REFERENCES facebook_ads_ad(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
);
