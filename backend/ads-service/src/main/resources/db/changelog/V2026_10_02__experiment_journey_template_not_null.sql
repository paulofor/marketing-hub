--liquibase formatted sql
--changeset repo:2026-10-02-experiment-journey-template-not-null dbms:mysql
--preconditions onFail:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM experiment WHERE journey_template_id IS NULL;
ALTER TABLE experiment MODIFY journey_template_id BIGINT NOT NULL;
