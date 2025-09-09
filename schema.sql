CREATE TABLE asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20),
    provider VARCHAR(20),
    external_id VARCHAR(100),
    status VARCHAR(20),
    url VARCHAR(500),
    payload TEXT,
    campaign_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE course_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_audience VARCHAR(255),
    transformation VARCHAR(255),
    macro_topics TEXT,
    modules TEXT,
    objectives TEXT,
    resources TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    objective LONGTEXT,
    url VARCHAR(255),
    phase VARCHAR(255),
    price DECIMAL(10,2),
    cost DECIMAL(10,2),
    observation LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    niche VARCHAR(255),
    avatar VARCHAR(255),
    instagram_account_id BIGINT,
    explicit_pain LONGTEXT,
    promise LONGTEXT,
    unique_mechanism LONGTEXT,
    tripwire LONGTEXT,
    risk_reversal LONGTEXT,
    social_proof LONGTEXT,
    checkout_monetization LONGTEXT,
    funnel LONGTEXT,
    creative_volume LONGTEXT,
    storytelling LONGTEXT,
    ai_cost DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE success_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description LONGTEXT,
    name VARCHAR(255),
    novo BOOLEAN DEFAULT TRUE,
    platform VARCHAR(20) NOT NULL DEFAULT 'COFRE',
    niche VARCHAR(255),
    avatar VARCHAR(255),
    audience_type VARCHAR(255),
    sales_page_url VARCHAR(500),
    instagram_url VARCHAR(500),
    facebook_url VARCHAR(500),
    youtube_url VARCHAR(500),
    instagram_account_id BIGINT,
    explicit_pain LONGTEXT,
    promise LONGTEXT,
    unique_mechanism LONGTEXT,
    tripwire LONGTEXT,
    risk_reversal LONGTEXT,
    social_proof LONGTEXT,
    checkout_monetization LONGTEXT,
    sales_funnel LONGTEXT,
    creative_volume LONGTEXT,
    storytelling LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE instagram_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instagram_account_id BIGINT,
    caption TEXT,
    media_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE market_niche (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description LONGTEXT,
    demand_volume LONGTEXT,
    promises LONGTEXT,
    offers LONGTEXT,
    hypotheses_to_generate INT,
    base_segmentation LONGTEXT,
    interests LONGTEXT,
    demographic_filters LONGTEXT,
    extra_tips LONGTEXT,
    chat_dialog_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE experiment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    niche_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    hypothesis VARCHAR(255),
    kpi_target_cpl DECIMAL(10,2) DEFAULT 45.00,
    stop_loss_cpl DECIMAL(10,2) DEFAULT 90.00,
    sample_size INT DEFAULT 1500,
    baseline_cvr DECIMAL(5,2) DEFAULT 3.00,
    target_cvr DECIMAL(5,2) DEFAULT 5.00,
    mde_percent DECIMAL(5,2) DEFAULT 40.0,
    creatives_to_generate INT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20),
    platform VARCHAR(50),
    audience_approved BOOLEAN DEFAULT FALSE,
    creative_approved BOOLEAN DEFAULT FALSE,
    sales_funnel_id BINARY(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE creative (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    headline VARCHAR(255),
    primary_text VARCHAR(255),
    image_url VARCHAR(500),
    image_hash VARCHAR(255),
    video_id VARCHAR(255),
    status VARCHAR(20),
    CONSTRAINT fk_creative_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id)
);

CREATE TABLE creative_variant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    type VARCHAR(20),
    asset_url VARCHAR(500),
    titles LONGTEXT,
    descriptions LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_creative_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id)
);

CREATE TABLE ad_set (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT,
    location VARCHAR(255),
    interests LONGTEXT,
    lookalikes LONGTEXT,
    budget DECIMAL(10,2),
    duration_days INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE metric_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creative_id BIGINT,
    ad_set_id BIGINT,
    impressions INT,
    clicks INT,
    cost DECIMAL(10,2),
    roas DECIMAL(10,2),
    ctr DOUBLE,
    cpa DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE landing_page (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    url VARCHAR(500),
    type VARCHAR(20),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255),
    channel VARCHAR(50),
    state VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT,
    origin VARCHAR(50),
    content LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE chat_dialog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500),
    description LONGTEXT,
    theme VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE prompt_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE prompt_attribute (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_entity_id BIGINT,
    name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_attribute_entity FOREIGN KEY (prompt_entity_id) REFERENCES prompt_entity(id)
);

CREATE TABLE prompt_entity_description (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_entity_id BIGINT,
    description LONGTEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_entity_description_entity FOREIGN KEY (prompt_entity_id) REFERENCES prompt_entity(id)
);

CREATE TABLE prompt_attribute_description (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_attribute_id BIGINT,
    description LONGTEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_attribute_description_attr FOREIGN KEY (prompt_attribute_id) REFERENCES prompt_attribute(id)
);

CREATE TABLE hypothesis_prompt_attribute_description (
    hypothesis_id BINARY(16) NOT NULL,
    prompt_attribute_description_id BIGINT NOT NULL,
    PRIMARY KEY (hypothesis_id, prompt_attribute_description_id),
    CONSTRAINT fk_hpah_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id),
    CONSTRAINT fk_hpah_description FOREIGN KEY (prompt_attribute_description_id) REFERENCES prompt_attribute_description(id)
);

ALTER TABLE hypothesis ADD COLUMN entrega LONGTEXT;

CREATE TABLE facebook_ads_campaign (
  id CHAR(36) NOT NULL,
  external_id VARCHAR(64),
  ad_account_id VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  objective VARCHAR(64) NOT NULL,
  status ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  budget_mode ENUM('CAMPAIGN','ADSET') NOT NULL,
  daily_budget_minor BIGINT UNSIGNED,
  lifetime_budget_minor BIGINT UNSIGNED,
  api_version VARCHAR(16),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE facebook_ads_campaign_special_ad_category (
  campaign_id CHAR(36) NOT NULL,
  category ENUM('NONE','CREDIT','EMPLOYMENT','HOUSING','ISSUES_ELECTIONS_POLITICS') NOT NULL,
  PRIMARY KEY (campaign_id, category),
  CONSTRAINT fk_facebook_ads_csac_campaign FOREIGN KEY (campaign_id) REFERENCES facebook_ads_campaign(id) ON DELETE CASCADE ON UPDATE RESTRICT
);

CREATE TABLE facebook_ads_campaign_special_ad_country (
  campaign_id CHAR(36) NOT NULL,
  country_iso2 CHAR(2) NOT NULL,
  PRIMARY KEY (campaign_id, country_iso2),
  CONSTRAINT fk_facebook_ads_csacountry_campaign FOREIGN KEY (campaign_id) REFERENCES facebook_ads_campaign(id) ON DELETE CASCADE ON UPDATE RESTRICT
);

CREATE TABLE facebook_ads_ad_set (
  id CHAR(36) NOT NULL,
  external_id VARCHAR(64),
  campaign_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  status ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  daily_budget_minor BIGINT UNSIGNED,
  lifetime_budget_minor BIGINT UNSIGNED,
  start_time DATETIME,
  end_time DATETIME,
  billing_event VARCHAR(32) NOT NULL,
  optimization_goal VARCHAR(64) NOT NULL,
  bid_strategy VARCHAR(64) NOT NULL,
  bid_amount_minor BIGINT UNSIGNED,
  promoted_object_json LONGTEXT,
  targeting_json LONGTEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_facebook_ads_adset_campaign FOREIGN KEY (campaign_id) REFERENCES facebook_ads_campaign(id) ON DELETE CASCADE ON UPDATE RESTRICT
);

CREATE TABLE facebook_ads_media_asset (
  id CHAR(36) NOT NULL,
  kind ENUM('IMAGE','VIDEO') NOT NULL,
  source_uri VARCHAR(1024),
  image_hash VARCHAR(128),
  video_id VARCHAR(64),
  width INT UNSIGNED,
  height INT UNSIGNED,
  duration_ms INT UNSIGNED,
  checksum VARCHAR(128),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE facebook_ads_ad_creative (
  id CHAR(36) NOT NULL,
  external_id VARCHAR(64),
  page_id VARCHAR(64) NOT NULL,
  instagram_user_id VARCHAR(64),
  kind ENUM('LINK','VIDEO','CAROUSEL') NOT NULL,
  link_data_json LONGTEXT,
  video_data_json LONGTEXT,
  carousel_data_json LONGTEXT,
  last_preview_url VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE facebook_ads_ad (
  id CHAR(36) NOT NULL,
  external_id VARCHAR(64),
  adset_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  creative_id CHAR(36) NOT NULL,
  status ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_facebook_ads_ad_adset FOREIGN KEY (adset_id) REFERENCES facebook_ads_ad_set(id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_facebook_ads_ad_creative FOREIGN KEY (creative_id) REFERENCES facebook_ads_ad_creative(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE facebook_ads_ad_tracking_utm (
  ad_id CHAR(36) NOT NULL,
  utm_source VARCHAR(64),
  utm_medium VARCHAR(64),
  utm_campaign VARCHAR(128),
  utm_content VARCHAR(128),
  utm_term VARCHAR(128),
  PRIMARY KEY (ad_id),
  CONSTRAINT fk_facebook_ads_utm_ad FOREIGN KEY (ad_id) REFERENCES facebook_ads_ad(id) ON DELETE CASCADE ON UPDATE RESTRICT
);
