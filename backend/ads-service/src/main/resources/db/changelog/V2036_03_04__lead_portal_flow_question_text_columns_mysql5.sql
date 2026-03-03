--liquibase formatted sql
--changeset repo:2036-03-04-lead-portal-flow-question-text-columns dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question' AND column_name IN ('title','description','placeholder') AND data_type <> 'longtext';
ALTER TABLE lead_portal_flow_question
    MODIFY COLUMN title LONGTEXT NOT NULL,
    MODIFY COLUMN description LONGTEXT NULL,
    MODIFY COLUMN placeholder LONGTEXT NULL;

--changeset repo:2036-03-04-lead-portal-flow-question-option-text-column dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow_question_option' AND column_name = 'option_value' AND data_type <> 'longtext';
ALTER TABLE lead_portal_flow_question_option
    MODIFY COLUMN option_value LONGTEXT NOT NULL;
