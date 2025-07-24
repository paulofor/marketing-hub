ALTER TABLE creative_variants
    ALTER COLUMN experiment_id SET NOT NULL;
ALTER TABLE creative_variants
    ADD CONSTRAINT fk_creative_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);
