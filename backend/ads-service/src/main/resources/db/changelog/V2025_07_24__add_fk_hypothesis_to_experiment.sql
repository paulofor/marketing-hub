-- liquibase formatted sql
-- changeset marketinghub:2025-07-24-add-fk-hypothesis-to-experiment
-- preconditions onFail:MARK_RAN onError:HALT
--   sql-check expectedResult:0 SELECT COUNT(*) 
--     FROM INFORMATION_SCHEMA.COLUMNS
--     WHERE TABLE_SCHEMA='marketinghubdb'
--       AND TABLE_NAME='experiment'
--       AND COLUMN_NAME='hypothesis_id';

ALTER TABLE experiment ADD COLUMN hypothesis_id BINARY(16) NOT NULL;

ALTER TABLE experiment
    ADD CONSTRAINT fk_experiment_hypothesis
    FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id);
