--liquibase formatted sql
--changeset repo:2037-04-07-experiment-pipeline-job-section-after-create-mysql5 dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'experiment_pipeline_generation_job';
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment_pipeline_generation_job' AND column_name = 'section' AND data_type = 'varchar' AND character_maximum_length >= 128;
ALTER TABLE experiment_pipeline_generation_job
    MODIFY COLUMN section VARCHAR(128) NOT NULL;
