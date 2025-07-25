ALTER TABLE ad_set
    MODIFY experiment_id BIGINT NOT NULL;
ALTER TABLE ad_set
    ADD CONSTRAINT fk_adset_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id);
