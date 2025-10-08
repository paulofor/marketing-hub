--liquibase formatted sql

--changeset repo:2026-09-05-remove-duplicate-template dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN COUNT(*) > 1 THEN 1 ELSE 0 END FROM journey_template WHERE name = 'Lifecycle Pós-Clique Lead Ads 14d';
DELETE jt
FROM journey_template jt
JOIN (
    SELECT MIN(id) AS keep_id
    FROM journey_template
    WHERE name = 'Lifecycle Pós-Clique Lead Ads 14d'
) keep ON 1 = 1
WHERE jt.name = 'Lifecycle Pós-Clique Lead Ads 14d'
  AND jt.id <> keep.keep_id;

--changeset repo:2026-09-05-remove-duplicate-journey dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN COUNT(*) > 1 THEN 1 ELSE 0 END FROM journey WHERE name = 'Lifecycle Pós-Clique Lead Ads 14d - Exemplo';
DELETE j
FROM journey j
JOIN (
    SELECT MIN(id) AS keep_id
    FROM journey
    WHERE name = 'Lifecycle Pós-Clique Lead Ads 14d - Exemplo'
) keep ON 1 = 1
WHERE j.name = 'Lifecycle Pós-Clique Lead Ads 14d - Exemplo'
  AND j.id <> keep.keep_id;

--changeset repo:2026-09-05-unique-template-name dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'journey_template' AND index_name = 'uk_journey_template_name';
ALTER TABLE journey_template
ADD CONSTRAINT uk_journey_template_name UNIQUE KEY (name);

--changeset repo:2026-09-05-unique-journey-name dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'journey' AND index_name = 'uk_journey_name';
ALTER TABLE journey
ADD CONSTRAINT uk_journey_name UNIQUE KEY (name);
