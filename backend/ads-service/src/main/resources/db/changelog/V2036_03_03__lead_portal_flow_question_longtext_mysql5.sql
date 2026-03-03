--liquibase formatted sql
--changeset repo:2036-03-03-lead-portal-flow-question-longtext dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question' AND column_name = 'title' AND data_type <> 'longtext';
ALTER TABLE lead_portal_flow_question
    MODIFY COLUMN title LONGTEXT NOT NULL,
    MODIFY COLUMN data_key LONGTEXT NOT NULL,
    MODIFY COLUMN description LONGTEXT NULL,
    MODIFY COLUMN placeholder LONGTEXT NULL;

--changeset repo:2036-03-03-lead-portal-flow-question-option-longtext dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question_option' AND column_name = 'option_value' AND data_type <> 'longtext';
ALTER TABLE lead_portal_flow_question_option
    MODIFY COLUMN option_value LONGTEXT NOT NULL;
