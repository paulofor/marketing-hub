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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE success_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description LONGTEXT,
    novo BOOLEAN DEFAULT TRUE,
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
