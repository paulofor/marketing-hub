--liquibase formatted sql
--changeset repo:2037-02-01-experiment-stage-metadata dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0
SELECT COUNT(*)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'experiment'
  AND column_name = 'stage';
ALTER TABLE experiment
    ADD COLUMN stage VARCHAR(32) NOT NULL DEFAULT 'AD' AFTER platform,
    ADD COLUMN primary_variable VARCHAR(191) NULL AFTER stage,
    ADD COLUMN primary_metric VARCHAR(191) NULL AFTER primary_variable;
UPDATE experiment SET stage = 'AD' WHERE stage IS NULL;
