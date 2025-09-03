-- liquibase formatted sql
-- changeset marketinghub:2025-07-01-add-fk-experiment-to-creative
ALTER TABLE creative_variant
    MODIFY experiment_id BIGINT NOT NULL;
ALTER TABLE creative_variant
    ADD CONSTRAINT fk_creative_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);
