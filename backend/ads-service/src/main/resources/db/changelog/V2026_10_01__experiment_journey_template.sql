--liquibase formatted sql
--changeset repo:2026-10-01-experiment-journey-template dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'sales_funnel_id';
ALTER TABLE experiment DROP FOREIGN KEY fk_experiment_sales_funnel;
ALTER TABLE experiment DROP COLUMN sales_funnel_id;
ALTER TABLE experiment ADD COLUMN journey_template_id BIGINT NULL AFTER metric_preset_id;
ALTER TABLE experiment ADD CONSTRAINT fk_experiment_journey_template FOREIGN KEY (journey_template_id) REFERENCES journey_template(id);
