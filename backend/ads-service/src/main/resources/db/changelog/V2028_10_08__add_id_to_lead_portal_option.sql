--liquibase formatted sql

--changeset repo:2028-10-08-add-id-to-lead-portal-option dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question_option' AND column_name = 'id'
ALTER TABLE lead_portal_flow_question_option DROP FOREIGN KEY fk_lead_portal_question_option;
ALTER TABLE lead_portal_flow_question_option
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id),
    ADD CONSTRAINT uq_lead_portal_question_option_order UNIQUE KEY (question_id, option_order);
ALTER TABLE lead_portal_flow_question_option
    ADD CONSTRAINT fk_lead_portal_question_option FOREIGN KEY (question_id) REFERENCES lead_portal_flow_question(id) ON DELETE CASCADE;
