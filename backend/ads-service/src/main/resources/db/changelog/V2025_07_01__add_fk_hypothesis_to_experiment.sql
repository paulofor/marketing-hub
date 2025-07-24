ALTER TABLE experiment
    MODIFY hypothesis_id BINARY(16) NOT NULL,
    ADD CONSTRAINT fk_experiment_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id);
