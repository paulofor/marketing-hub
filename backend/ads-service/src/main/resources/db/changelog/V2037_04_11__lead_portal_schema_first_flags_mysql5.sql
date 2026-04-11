--liquibase formatted sql
--changeset repo:2037-04-11-lead-portal-schema-first-flags dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'schema_first';
ALTER TABLE lead_portal_flow
    ADD COLUMN schema_first TINYINT(1) NOT NULL DEFAULT 0;

--changeset repo:2037-04-11-experiment-schema-first-lead-portal-enabled dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'schema_first_lead_portal_enabled';
ALTER TABLE experiment
    ADD COLUMN schema_first_lead_portal_enabled TINYINT(1) NOT NULL DEFAULT 0;
