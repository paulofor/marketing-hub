--liquibase formatted sql

--changeset repo:2028-10-07-create-lead-portal-submission dbms:mysql
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_submission';
CREATE TABLE lead_portal_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id BIGINT NOT NULL,
    experiment_id BIGINT NULL,
    lead_id BINARY(16) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    source VARCHAR(64) NULL,
    primary_contact_name VARCHAR(255) NULL,
    primary_contact_email VARCHAR(320) NULL,
    primary_contact_phone VARCHAR(40) NULL,
    utm_source VARCHAR(100) NULL,
    utm_medium VARCHAR(100) NULL,
    utm_campaign VARCHAR(150) NULL,
    utm_content VARCHAR(150) NULL,
    utm_term VARCHAR(150) NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_portal_submission_flow FOREIGN KEY (flow_id) REFERENCES lead_portal_flow(id),
    CONSTRAINT fk_lead_portal_submission_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id),
    CONSTRAINT fk_lead_portal_submission_lead FOREIGN KEY (lead_id) REFERENCES `lead`(id)
) ENGINE=InnoDB;

--changeset repo:2028-10-07-create-lead-portal-answer dbms:mysql
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_submission_answer';
CREATE TABLE lead_portal_submission_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    data_key_snapshot VARCHAR(120) NOT NULL,
    text_value LONGTEXT NULL,
    number_value DECIMAL(18,4) NULL,
    date_value DATE NULL,
    boolean_value TINYINT(1) NULL,
    selected_option_id BIGINT NULL,
    asset_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_portal_answer_submission FOREIGN KEY (submission_id) REFERENCES lead_portal_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_portal_answer_question FOREIGN KEY (question_id) REFERENCES lead_portal_flow_question(id),
    CONSTRAINT fk_lead_portal_answer_option FOREIGN KEY (selected_option_id) REFERENCES lead_portal_flow_question_option(id),
    CONSTRAINT fk_lead_portal_answer_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    CONSTRAINT uq_lead_portal_submission_question UNIQUE KEY (submission_id, question_id)
) ENGINE=InnoDB;

--changeset repo:2028-10-07-create-lead-portal-answer-option dbms:mysql
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_submission_answer_option';
CREATE TABLE lead_portal_submission_answer_option (
    answer_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    PRIMARY KEY (answer_id, option_id),
    CONSTRAINT fk_lead_portal_answer_option_answer FOREIGN KEY (answer_id) REFERENCES lead_portal_submission_answer(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_portal_answer_option_option FOREIGN KEY (option_id) REFERENCES lead_portal_flow_question_option(id)
) ENGINE=InnoDB;

--changeset repo:2028-10-07-lead-portal-indexes dbms:mysql
CREATE INDEX idx_lead_portal_submission_flow ON lead_portal_submission(flow_id, submitted_at DESC);
CREATE INDEX idx_lead_portal_submission_lead ON lead_portal_submission(lead_id);
CREATE INDEX idx_lead_portal_submission_experiment ON lead_portal_submission(experiment_id);
CREATE INDEX idx_lead_portal_answer_submission ON lead_portal_submission_answer(submission_id);
CREATE INDEX idx_lead_portal_answer_question ON lead_portal_submission_answer(question_id);
