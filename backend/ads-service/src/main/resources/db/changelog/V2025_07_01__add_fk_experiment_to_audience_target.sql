-- liquibase formatted sql
-- changeset marketinghub:2025-07-01-add-fk-experiment-to-audience-target
ALTER TABLE ad_set
    MODIFY experiment_id BIGINT NOT NULL;
ALTER TABLE ad_set
    ADD CONSTRAINT fk_adset_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);
