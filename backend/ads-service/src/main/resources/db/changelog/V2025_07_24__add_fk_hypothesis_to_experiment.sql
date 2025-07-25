-- liquibase formatted sql

--changeset marketinghub:2025-07-24-col
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0
-- SELECT COUNT(*) 
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME   = 'experiment'
--    AND COLUMN_NAME  = 'hypothesis_id';

ALTER TABLE experiment
  ADD COLUMN hypothesis_id BINARY(16) NOT NULL;

--changeset marketinghub:2025-07-24-fk
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0
-- SELECT COUNT(*)
--   FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
--  WHERE CONSTRAINT_SCHEMA = DATABASE()
--    AND TABLE_NAME        = 'experiment'
--    AND CONSTRAINT_NAME   = 'fk_experiment_hypothesis'
--    AND CONSTRAINT_TYPE   = 'FOREIGN KEY';

ALTER TABLE experiment
  ADD CONSTRAINT fk_experiment_hypothesis
  FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id)
  ON DELETE CASCADE;
