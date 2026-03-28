--liquibase formatted sql
--changeset repo:2037-03-28-add-openai-request-payload-to-experiment-learning dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment_learning' AND COLUMN_NAME = 'openai_request_payload_json';
ALTER TABLE experiment_learning
    ADD COLUMN openai_request_payload_json LONGTEXT NULL AFTER suggestions_json;
