-- liquibase formatted sql
-- changeset marketinghub:2025-07-24-add-fk-hypothesis-to-experiment
-- preconditions onFail:MARK_RAN
--    <not>
--        <columnExists tableName="experiment" columnName="hypothesis_id"/>
--    </not>
ALTER TABLE experiment ADD COLUMN hypothesis_id BINARY(16) NOT NULL;
ALTER TABLE experiment
    ADD CONSTRAINT fk_experiment_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id);
