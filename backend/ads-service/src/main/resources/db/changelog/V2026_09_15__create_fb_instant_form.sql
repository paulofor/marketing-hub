--liquibase formatted sql
--changeset marketinghub:2026-09-15-create-fb-instant-form dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'fb_instant_form';
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

--changeset marketinghub:2026-09-15-add-instant-form-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'facebook_instant_form_id';
ALTER TABLE experiment
    ADD COLUMN facebook_instant_form_id BIGINT AFTER facebook_page_id,
    ADD CONSTRAINT fk_experiment_fb_instant_form FOREIGN KEY (facebook_instant_form_id) REFERENCES fb_instant_form(id);
