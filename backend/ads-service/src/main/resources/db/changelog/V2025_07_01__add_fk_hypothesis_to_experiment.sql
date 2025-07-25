-- liquibase formatted sql
-- changeset marketinghub:2025-07-01-add-fk-hypothesis-to-experiment
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND CONSTRAINT_NAME = 'fk_experiment_hypothesis' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
ALTER TABLE experiment
    MODIFY hypothesis_id BINARY(16) NOT NULL;
ALTER TABLE experiment
    ADD CONSTRAINT fk_experiment_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id);
