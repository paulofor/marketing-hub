-- liquibase formatted sql
-- changeset marketinghub:2025-07-29-drop-experiment-id
-- preconditions onFail:MARK_RAN
--    <columnExists tableName="hypothesis" columnName="experiment_id"/>
ALTER TABLE hypothesis DROP COLUMN experiment_id;
