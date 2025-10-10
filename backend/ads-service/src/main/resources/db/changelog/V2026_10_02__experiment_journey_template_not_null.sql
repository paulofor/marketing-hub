--liquibase formatted sql
--changeset repo:2026-10-02-experiment-journey-template-not-null dbms:mysql
--preconditions onFail:HALT
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS(SELECT 1 FROM experiment WHERE journey_template_id IS NULL) AND NOT EXISTS(SELECT 1 FROM journey_template) THEN 1 ELSE 0 END;
SET @default_journey_template_id = (
    SELECT COALESCE(
        (SELECT id FROM journey_template WHERE name = 'Lifecycle Pós-Clique Lead Ads 14d' ORDER BY id DESC LIMIT 1),
        (SELECT id FROM journey_template ORDER BY id ASC LIMIT 1)
    )
);
UPDATE experiment
SET journey_template_id = @default_journey_template_id
WHERE journey_template_id IS NULL;
ALTER TABLE experiment MODIFY journey_template_id BIGINT NOT NULL;
