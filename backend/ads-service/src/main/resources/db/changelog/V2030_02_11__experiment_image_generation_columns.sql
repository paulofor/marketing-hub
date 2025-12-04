--liquibase formatted sql
--changeset repo:2030-02-11-experiment-image-generation-columns dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'image_model_id';
ALTER TABLE experiment
    ADD COLUMN image_model_id BIGINT NULL,
    ADD COLUMN image_model_quality_id BIGINT NULL,
    ADD CONSTRAINT fk_experiment_image_model FOREIGN KEY (image_model_id) REFERENCES image_generation_model(id),
    ADD CONSTRAINT fk_experiment_image_quality FOREIGN KEY (image_model_quality_id) REFERENCES image_generation_quality(id);

ALTER TABLE flow_submission_image_package
    ADD COLUMN image_model_id BIGINT NULL,
    ADD COLUMN image_model_quality_id BIGINT NULL,
    ADD COLUMN image_orientation VARCHAR(16) NULL,
    ADD COLUMN image_width INT NULL,
    ADD COLUMN image_height INT NULL,
    ADD COLUMN image_unit_price_usd DECIMAL(10,5) NULL,
    ADD COLUMN image_total_price_usd DECIMAL(12,5) NULL,
    ADD COLUMN image_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD CONSTRAINT fk_flow_image_package_model FOREIGN KEY (image_model_id) REFERENCES image_generation_model(id),
    ADD CONSTRAINT fk_flow_image_package_quality FOREIGN KEY (image_model_quality_id) REFERENCES image_generation_quality(id);
