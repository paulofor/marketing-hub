-- liquibase formatted sql
-- changeset marketinghub:2025-07-01-add-fk-experiment-to-creative
ALTER TABLE creative_variants
    MODIFY experiment_id BIGINT NOT NULL;
ALTER TABLE creative_variants
    ADD CONSTRAINT fk_creative_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);
