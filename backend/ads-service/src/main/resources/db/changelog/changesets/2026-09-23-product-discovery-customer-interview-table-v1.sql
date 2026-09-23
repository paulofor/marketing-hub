CREATE TABLE IF NOT EXISTS product_discovery_customer_interview (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cycle_id BIGINT NOT NULL,
  opportunity_id BIGINT NOT NULL,
  anonymous_participant_code VARCHAR(7) NOT NULL,
  outcome VARCHAR(24) NOT NULL,
  occurred_on DATE NOT NULL,
  consent_captured_at DATETIME(6) NOT NULL,
  purchase_situation LONGTEXT NOT NULL,
  desired_result LONGTEXT NOT NULL,
  difficulty LONGTEXT NOT NULL,
  alternative_tried LONGTEXT NOT NULL,
  amount_spent DECIMAL(12,2) NULL,
  currency VARCHAR(3) NULL,
  remaining_difficulty LONGTEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pd_customer_interview_cycle_code (cycle_id, anonymous_participant_code),
  KEY idx_pd_customer_interview_opportunity (opportunity_id),
  KEY idx_pd_customer_interview_cycle_outcome (cycle_id, outcome),
  CONSTRAINT fk_pd_customer_interview_cycle
    FOREIGN KEY (cycle_id) REFERENCES product_discovery_cycle (id) ON DELETE CASCADE,
  CONSTRAINT fk_pd_customer_interview_opportunity
    FOREIGN KEY (opportunity_id) REFERENCES product_discovery_opportunity (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
