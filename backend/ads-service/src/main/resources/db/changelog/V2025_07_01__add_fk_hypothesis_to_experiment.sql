ALTER TABLE experiment
    ALTER COLUMN hypothesis_id SET NOT NULL;
ALTER TABLE experiment
    ADD CONSTRAINT fk_experiment_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id);
