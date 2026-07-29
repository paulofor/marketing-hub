CREATE TABLE IF NOT EXISTS pde_access_grant (
  token VARCHAR(120) NOT NULL,
  product_slug VARCHAR(120) NOT NULL,
  email VARCHAR(191) NOT NULL,
  source VARCHAR(80) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (token),
  KEY idx_pde_access_grant_product_email (product_slug, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pde_access_mission_completion (
  access_token VARCHAR(120) NOT NULL,
  mission_id VARCHAR(120) NOT NULL,
  completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (access_token, mission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pde_access_mission_interaction_answer (
  access_token VARCHAR(120) NOT NULL,
  mission_id VARCHAR(120) NOT NULL,
  question_key VARCHAR(120) NOT NULL,
  answer_text TEXT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (access_token, mission_id, question_key)
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
  KEY idx_pde_funnel_product_event_time (product_slug, event_type, occurred_at),
  KEY idx_pde_funnel_session_time (session_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
