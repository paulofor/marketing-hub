--liquibase formatted sql
--changeset repo:2026-03-28-hypothesis-framework-job-stage dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hypothesis_framework_generation_job' AND column_name = 'stage';
ALTER TABLE hypothesis_framework_generation_job
    ADD COLUMN stage VARCHAR(32) NOT NULL DEFAULT 'WAITING_AI_WORKER' AFTER status;
