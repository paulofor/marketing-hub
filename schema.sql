CREATE TABLE asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255),
    provider VARCHAR(255),
    external_id VARCHAR(255),
    status VARCHAR(255),
    url VARCHAR(1024),
    payload LONGTEXT,
    campaign_id BIGINT,
    model VARCHAR(255),
    prompt LONGTEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6)
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

CREATE TABLE microservice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    base_url VARCHAR(512),
    category VARCHAR(100),
    status VARCHAR(50),
    owner VARCHAR(255),
    documentation_url VARCHAR(512),
    health_check_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_worker_generation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100),
    model VARCHAR(191),
    prompt LONGTEXT,
    raw_response LONGTEXT,
    input_tokens INT,
    output_tokens INT,
    cost_usd DECIMAL(10,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    niche VARCHAR(255),
    avatar VARCHAR(255),
    instagram_account_id BIGINT,
    market_niche_id BIGINT,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id)
);

CREATE TABLE member_area (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(255),
    access_url VARCHAR(500),
    description LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_area_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE app_idea (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    market_niche_id BIGINT NOT NULL,
    target_audience VARCHAR(255),
    problem_to_solve LONGTEXT,
    value_proposition LONGTEXT,
    core_features LONGTEXT,
    differentiator LONGTEXT,
    monetization LONGTEXT,
    go_to_market LONGTEXT,
    technology_stack LONGTEXT,
    model VARCHAR(255),
    prompt LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_idea_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id)
);

CREATE TABLE success_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description LONGTEXT,
    name VARCHAR(255),
    novo BOOLEAN DEFAULT TRUE,
    platform VARCHAR(20) NOT NULL DEFAULT 'COFRE',
    generate_niche_hypothesis BOOLEAN DEFAULT FALSE,
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
    interest_category VARCHAR(255),
    role_category VARCHAR(255),
    demand_volume LONGTEXT,
    promises LONGTEXT,
    offers LONGTEXT,
    hypotheses_to_generate INT,
    audiences_to_generate INT,
    base_segmentation LONGTEXT,
    interests LONGTEXT,
    demographic_filters LONGTEXT,
    extra_tips LONGTEXT,
    chat_dialog_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE lead_portal_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE lead_portal_flow_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    data_key VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    required BOOLEAN NOT NULL,
    description VARCHAR(500),
    placeholder VARCHAR(255),
    position_index INT NOT NULL,
    CONSTRAINT fk_lead_portal_question_flow FOREIGN KEY (flow_id) REFERENCES lead_portal_flow(id) ON DELETE CASCADE,
    CONSTRAINT uq_lead_portal_question_key UNIQUE (flow_id, data_key)
);

CREATE TABLE lead_portal_flow_question_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_order INT NOT NULL,
    option_value VARCHAR(255) NOT NULL,
    CONSTRAINT fk_lead_portal_question_option FOREIGN KEY (question_id) REFERENCES lead_portal_flow_question(id) ON DELETE CASCADE,
    CONSTRAINT uq_lead_portal_question_option_order UNIQUE (question_id, option_order)
);

CREATE TABLE lead_portal_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    experiment_id BIGINT,
    lead_id BINARY(16),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    source VARCHAR(64),
    primary_contact_name VARCHAR(255),
    primary_contact_email VARCHAR(320),
    primary_contact_phone VARCHAR(40),
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(150),
    utm_content VARCHAR(150),
    utm_term VARCHAR(150),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_portal_submission_flow FOREIGN KEY (flow_id) REFERENCES lead_portal_flow(id),
    CONSTRAINT fk_lead_portal_submission_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id),
    CONSTRAINT fk_lead_portal_submission_lead FOREIGN KEY (lead_id) REFERENCES `lead`(id)
);

CREATE TABLE lead_portal_submission_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    data_key_snapshot VARCHAR(120) NOT NULL,
    text_value LONGTEXT,
    number_value DECIMAL(18,4),
    date_value DATE,
    boolean_value TINYINT(1),
    selected_option_id BIGINT,
    asset_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_portal_answer_submission FOREIGN KEY (submission_id) REFERENCES lead_portal_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_portal_answer_question FOREIGN KEY (question_id) REFERENCES lead_portal_flow_question(id),
    CONSTRAINT fk_lead_portal_answer_option FOREIGN KEY (selected_option_id) REFERENCES lead_portal_flow_question_option(id),
    CONSTRAINT fk_lead_portal_answer_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    CONSTRAINT uq_lead_portal_submission_question UNIQUE (submission_id, question_id)
);

CREATE TABLE lead_portal_submission_answer_option (
    answer_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    PRIMARY KEY (answer_id, option_id),
    CONSTRAINT fk_lead_portal_answer_option_answer FOREIGN KEY (answer_id) REFERENCES lead_portal_submission_answer(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_portal_answer_option_option FOREIGN KEY (option_id) REFERENCES lead_portal_flow_question_option(id)
);

CREATE INDEX idx_lead_portal_submission_flow ON lead_portal_submission(flow_id, submitted_at DESC);
CREATE INDEX idx_lead_portal_submission_lead ON lead_portal_submission(lead_id);
CREATE INDEX idx_lead_portal_submission_experiment ON lead_portal_submission(experiment_id);
CREATE INDEX idx_lead_portal_answer_submission ON lead_portal_submission_answer(submission_id);
CREATE INDEX idx_lead_portal_answer_question ON lead_portal_submission_answer(question_id);

CREATE TABLE image_generation_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    api_model VARCHAR(128) NOT NULL,
    description LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE image_generation_quality (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    api_quality VARCHAR(32),
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_generation_quality_model FOREIGN KEY (model_id) REFERENCES image_generation_model(id),
    CONSTRAINT uq_image_generation_quality UNIQUE (model_id, code)
);

CREATE TABLE image_generation_price (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quality_id BIGINT NOT NULL,
    orientation VARCHAR(16) NOT NULL,
    width INT,
    height INT,
    size_label VARCHAR(32),
    unit_price_usd DECIMAL(10,5),
    preferred TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_generation_price_quality FOREIGN KEY (quality_id) REFERENCES image_generation_quality(id)
);

CREATE TABLE experiment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    niche_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    hypothesis VARCHAR(255),
    facebook_page_id BIGINT,
    facebook_instant_form_id BIGINT,
    follow_up_action_url VARCHAR(512),
    lead_portal_flow_id BIGINT,
    image_model_id BIGINT,
    image_model_quality_id BIGINT,
    instagram_account_id BIGINT,
    kpi_target_cpl DECIMAL(10,2) DEFAULT 45.00,
    stop_loss_cpl DECIMAL(10,2) DEFAULT 90.00,
    sample_size INT DEFAULT 1500,
    baseline_cvr DECIMAL(5,2) DEFAULT 3.00,
    target_cvr DECIMAL(5,2) DEFAULT 5.00,
    mde_percent DECIMAL(5,2) DEFAULT 40.0,
    creatives_to_generate INT,
    instant_forms_to_generate INT,
    emails_to_generate INT,
    sample_emails_to_generate INT,
    deliverables_to_generate INT,
    lead_portal_flows_to_generate INT,
    images_per_package INT DEFAULT 20,
    send_images_as_zip BOOLEAN DEFAULT TRUE,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20),
    platform VARCHAR(50),
    creative_approved BOOLEAN DEFAULT FALSE,
    journey_template_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_facebook_page FOREIGN KEY (facebook_page_id) REFERENCES fb_page(id),
    CONSTRAINT fk_experiment_fb_instant_form FOREIGN KEY (facebook_instant_form_id) REFERENCES fb_instant_form(id),
    CONSTRAINT fk_experiment_instagram_account FOREIGN KEY (instagram_account_id) REFERENCES ig_account(id),
    CONSTRAINT fk_experiment_journey_template FOREIGN KEY (journey_template_id) REFERENCES journey_template(id),
    CONSTRAINT fk_experiment_lead_portal_flow FOREIGN KEY (lead_portal_flow_id) REFERENCES lead_portal_flow(id),
    CONSTRAINT fk_experiment_image_model FOREIGN KEY (image_model_id) REFERENCES image_generation_model(id),
    CONSTRAINT fk_experiment_image_model_quality FOREIGN KEY (image_model_quality_id) REFERENCES image_generation_quality(id)
);

CREATE TABLE experiment_sample_email (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    preview_text VARCHAR(255),
    body LONGTEXT,
    call_to_action VARCHAR(500),
    model VARCHAR(128),
    prompt LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sample_email_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id)
);

CREATE TABLE audience (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description LONGTEXT,
    prompt LONGTEXT,
    model VARCHAR(255),
    approved BOOLEAN DEFAULT FALSE,
    market_niche_id BIGINT,
    hypothesis_id BINARY(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_audience_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id),
    CONSTRAINT fk_audience_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id)
);

CREATE TABLE fb_instant_form (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hypothesis_id BINARY(16) NOT NULL,
    page_id BIGINT NOT NULL,
    form_id VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    locale VARCHAR(12),
    leads_count BIGINT,
    created_time DATETIME,
    updated_time DATETIME,
    follow_up_action_url VARCHAR(512),
    privacy_policy_url VARCHAR(512),
    model VARCHAR(128),
    prompt LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fb_instant_form_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id),
    CONSTRAINT fk_fb_instant_form_page FOREIGN KEY (page_id) REFERENCES fb_page(id),
    CONSTRAINT uq_fb_instant_form UNIQUE (form_id)
);

CREATE TABLE general_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    value LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_general_setting_name UNIQUE (name)
);

CREATE TABLE creative (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    headline VARCHAR(255),
    primary_text VARCHAR(255),
    image_url VARCHAR(500),
    ad_format VARCHAR(32),
    description VARCHAR(255),
    call_to_action VARCHAR(32),
    destination_url VARCHAR(512),
    instagram_user_id VARCHAR(64),
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
    targeting_json LONGTEXT,
    budget DECIMAL(10,2),
    duration_days INT,
    prompt LONGTEXT,
    model VARCHAR(255),
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
  experiment_id BIGINT NOT NULL,
  facebook_account_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  objective VARCHAR(64) NOT NULL,
  status ENUM('PAUSED','ACTIVE','ARCHIVED','DELETED') NOT NULL DEFAULT 'PAUSED',
  budget_mode ENUM('CAMPAIGN','ADSET') NOT NULL,
  daily_budget_minor BIGINT UNSIGNED,
  lifetime_budget_minor BIGINT UNSIGNED,
  api_version VARCHAR(16),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_facebook_ads_campaign_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id),
  CONSTRAINT fk_facebook_ads_campaign_account FOREIGN KEY (facebook_account_id) REFERENCES fb_account(id)
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

CREATE TABLE flow_submission_image_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    planned_outputs INT NULL,
    free_images INT NOT NULL DEFAULT 0,
    model VARCHAR(255),
    prompt LONGTEXT NOT NULL,
    failure_reason LONGTEXT NULL,
    image_model_id BIGINT NULL,
    image_model_quality_id BIGINT NULL,
    image_orientation VARCHAR(16),
    image_width INT,
    image_height INT,
    image_unit_price_usd DECIMAL(10,5),
    image_total_price_usd DECIMAL(12,5),
    image_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    zip_object_key VARCHAR(512) NULL,
    zip_size_bytes BIGINT NULL,
    zip_generated_at TIMESTAMP NULL,
    zip_last_error TEXT NULL,
    zip_attempts INT NOT NULL DEFAULT 0,
    zip_last_attempt TIMESTAMP NULL,
    notified_at TIMESTAMP NULL,
    notification_attempts INT NOT NULL DEFAULT 0,
    notification_last_attempt TIMESTAMP NULL,
    notification_last_error TEXT NULL,
    email_opened_at TIMESTAMP NULL,
    images_viewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_package_submission FOREIGN KEY (submission_id) REFERENCES flow_submissions(id),
    CONSTRAINT fk_flow_submission_image_package_model FOREIGN KEY (image_model_id) REFERENCES image_generation_model(id),
    CONSTRAINT fk_flow_submission_image_package_quality FOREIGN KEY (image_model_quality_id) REFERENCES image_generation_quality(id)
);

CREATE TABLE flow_submission_image_package_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_package_status_history_package FOREIGN KEY (package_id) REFERENCES flow_submission_image_package(id) ON DELETE CASCADE
);
CREATE INDEX idx_flow_submission_image_package_status_history_package
    ON flow_submission_image_package_status_history(package_id, created_at, id);

CREATE TABLE flow_submission_image_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    access_type VARCHAR(20) NOT NULL,
    position_index INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_item_package FOREIGN KEY (package_id) REFERENCES flow_submission_image_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_flow_submission_image_item_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    CONSTRAINT uq_flow_submission_image_item_order UNIQUE KEY (package_id, position_index)
);

CREATE TABLE flow_submission_image_watermark (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    optimized_asset_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_watermark_item FOREIGN KEY (item_id) REFERENCES flow_submission_image_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_flow_submission_image_watermark_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    CONSTRAINT fk_flow_submission_image_watermark_opt_asset FOREIGN KEY (optimized_asset_id) REFERENCES asset(id)
);
CREATE INDEX idx_flow_submission_image_watermark_opt_asset ON flow_submission_image_watermark(optimized_asset_id);

CREATE INDEX idx_flow_submission_image_package_submission ON flow_submission_image_package(submission_id, created_at DESC);
CREATE INDEX idx_flow_image_package_zip_status ON flow_submission_image_package(zip_object_key, status, updated_at);
CREATE INDEX idx_flow_image_package_status_created_at ON flow_submission_image_package(status, created_at);
CREATE INDEX idx_flow_submission_image_item_package ON flow_submission_image_item(package_id);

CREATE TABLE microservice_exception_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    microservice_id BIGINT NOT NULL,
    exception_type VARCHAR(255),
    message LONGTEXT,
    stack_trace LONGTEXT,
    severity VARCHAR(20),
    service_version VARCHAR(100),
    hostname VARCHAR(255),
    context LONGTEXT,
    occurred_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_microservice_exception_microservice FOREIGN KEY (microservice_id) REFERENCES microservice(id)
);
CREATE INDEX idx_microservice_exception_microservice ON microservice_exception_log (microservice_id, occurred_at DESC);
CREATE INDEX idx_microservice_exception_severity ON microservice_exception_log (severity);
