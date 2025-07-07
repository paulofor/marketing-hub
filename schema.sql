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
