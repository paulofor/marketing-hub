ALTER TABLE ad_set
    ALTER COLUMN experiment_id SET NOT NULL;
ALTER TABLE ad_set
    ADD CONSTRAINT fk_adset_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);
