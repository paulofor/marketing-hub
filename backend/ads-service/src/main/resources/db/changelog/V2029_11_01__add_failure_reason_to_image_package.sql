--liquibase formatted sql
--changeset repo:2029-11-01-add-failure-reason-to-image-packages dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0
--  SELECT COUNT(*)
--  FROM information_schema.columns
--  WHERE table_schema = DATABASE()
--    AND table_name = 'flow_submission_image_package'
--    AND column_name = 'failure_reason'
ALTER TABLE flow_submission_image_package
    ADD COLUMN failure_reason LONGTEXT NULL;
