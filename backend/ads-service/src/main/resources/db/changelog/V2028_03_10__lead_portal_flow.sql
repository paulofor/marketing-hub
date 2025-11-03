--liquibase formatted sql
--changeset repo:2028-03-10-lead-portal-flow dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow'
CREATE TABLE lead_portal_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_lead_portal_flow_slug UNIQUE KEY (slug)
);

--changeset repo:2028-03-10-lead-portal-flow-questions dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question'
CREATE TABLE lead_portal_flow_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    data_key VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    required TINYINT(1) NOT NULL,
    description VARCHAR(500),
    placeholder VARCHAR(255),
    position_index INT NOT NULL,
    CONSTRAINT fk_lead_portal_question_flow FOREIGN KEY (flow_id) REFERENCES lead_portal_flow(id) ON DELETE CASCADE,
    CONSTRAINT uq_lead_portal_question_key UNIQUE KEY (flow_id, data_key)
);

CREATE TABLE lead_portal_flow_question_option (
    question_id BIGINT NOT NULL,
    option_order INT NOT NULL,
    value VARCHAR(255) NOT NULL,
    PRIMARY KEY (question_id, option_order),
    CONSTRAINT fk_lead_portal_question_option FOREIGN KEY (question_id) REFERENCES lead_portal_flow_question(id) ON DELETE CASCADE
);

--changeset repo:2028-03-10-experiment-lead-portal-flow dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'lead_portal_flow_id'
ALTER TABLE experiment ADD COLUMN lead_portal_flow_id BIGINT NULL;
ALTER TABLE experiment
    ADD CONSTRAINT fk_experiment_lead_portal_flow FOREIGN KEY (lead_portal_flow_id)
        REFERENCES lead_portal_flow(id);
