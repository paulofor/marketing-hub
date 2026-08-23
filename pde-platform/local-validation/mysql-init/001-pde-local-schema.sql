CREATE TABLE IF NOT EXISTS pde_access_grant (
  token VARCHAR(36) NOT NULL,
  product_slug VARCHAR(191) NOT NULL,
  email VARCHAR(320) NOT NULL,
  normalized_email VARCHAR(320) NOT NULL,
  source VARCHAR(40) NOT NULL,
  experience_version VARCHAR(80) NOT NULL DEFAULT '',
  paid_at DATETIME NULL,
  expires_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (token),
  UNIQUE KEY uk_pde_access_product_email (product_slug, normalized_email),
  KEY idx_pde_access_product (product_slug),
  KEY idx_pde_access_email (normalized_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pde_access_mission_completion (
  id BIGINT NOT NULL AUTO_INCREMENT,
  access_token VARCHAR(36) NOT NULL,
  mission_id VARCHAR(191) NOT NULL,
  completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pde_access_mission (access_token, mission_id),
  KEY idx_pde_access_mission_token (access_token),
  CONSTRAINT fk_pde_access_mission_grant
    FOREIGN KEY (access_token) REFERENCES pde_access_grant (token)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pde_access_mission_interaction_answer (
  id BIGINT NOT NULL AUTO_INCREMENT,
  access_token VARCHAR(36) NOT NULL,
  product_slug VARCHAR(191) NOT NULL,
  mission_id VARCHAR(191) NOT NULL,
  question_key VARCHAR(100) NOT NULL,
  answer_text LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pde_mission_interaction_answer (access_token, mission_id, question_key),
  KEY idx_pde_mission_interaction_product (product_slug, mission_id),
  KEY idx_pde_mission_interaction_token (access_token),
  CONSTRAINT fk_pde_mission_interaction_grant
    FOREIGN KEY (access_token) REFERENCES pde_access_grant (token)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pde_funnel_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  product_slug VARCHAR(120) NOT NULL,
  experience_version VARCHAR(80) NULL,
  access_token VARCHAR(120) NULL,
  email VARCHAR(191) NULL,
  normalized_email VARCHAR(191) NULL,
  event_type VARCHAR(80) NOT NULL,
  provider VARCHAR(80) NULL,
  source VARCHAR(120) NULL,
  page_url VARCHAR(1024) NULL,
  client_ip VARCHAR(45) NULL,
  user_agent VARCHAR(512) NULL,
  traffic_quality VARCHAR(40) NULL,
  traffic_quality_reason VARCHAR(120) NULL,
  traffic_provider VARCHAR(80) NULL,
  referrer_url VARCHAR(1024) NULL,
  session_id VARCHAR(64) NULL,
  visitor_id VARCHAR(64) NULL,
  utm_source VARCHAR(120) NULL,
  utm_medium VARCHAR(120) NULL,
  utm_campaign VARCHAR(191) NULL,
  utm_content VARCHAR(191) NULL,
  utm_term VARCHAR(191) NULL,
  device_type VARCHAR(40) NULL,
  screen_width INT NULL,
  screen_height INT NULL,
  viewport_width INT NULL,
  viewport_height INT NULL,
  visible_ms BIGINT NULL,
  section_id VARCHAR(120) NULL,
  action_name VARCHAR(120) NULL,
  metadata_json TEXT NULL,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pde_funnel_event_event_id (event_id),
  KEY idx_pde_funnel_product_version_time (product_slug, experience_version, occurred_at),
  KEY idx_pde_funnel_product_quality_time (product_slug, traffic_quality, occurred_at),
  KEY idx_pde_funnel_product_quality_utm_session (
    product_slug(80), traffic_quality(40), utm_source(60), utm_medium(60),
    utm_campaign(80), utm_content(80), session_id(64), occurred_at
  ),
  KEY idx_pde_funnel_product_quality_session_time (
    product_slug(80), traffic_quality(40), session_id(64), occurred_at
  ),
  KEY idx_pde_funnel_product_event_time (product_slug, event_type, occurred_at),
  KEY idx_pde_funnel_session_time (session_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pde_payment_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  provider VARCHAR(40) NOT NULL,
  transaction_id VARCHAR(191) NOT NULL,
  product_slug VARCHAR(191) NOT NULL,
  offer_hash VARCHAR(191) NOT NULL,
  amount_cents INT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  payment_status VARCHAR(40) NOT NULL,
  buyer_reference_hash CHAR(64) NOT NULL,
  access_reference_hash CHAR(64) NULL,
  verified_at DATETIME NOT NULL,
  access_released_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pde_payment_provider_transaction (provider, transaction_id),
  KEY idx_pde_payment_product_verified (product_slug, verified_at),
  KEY idx_pde_payment_buyer_reference (buyer_reference_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
