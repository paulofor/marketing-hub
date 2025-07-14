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
    price DECIMAL(10,2),
    cost DECIMAL(10,2),
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
    niche VARCHAR(255),
    avatar VARCHAR(255),
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
    funnel LONGTEXT,
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
    base_segmentation LONGTEXT,
    interests LONGTEXT,
    demographic_filters LONGTEXT,
    extra_tips LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
