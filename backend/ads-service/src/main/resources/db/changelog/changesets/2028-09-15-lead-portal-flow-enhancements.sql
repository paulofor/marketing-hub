--liquibase formatted sql
--changeset repo:2028-09-15-experiment-lead-portal-generation dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'lead_portal_flows_to_generate'
ALTER TABLE experiment
    ADD COLUMN lead_portal_flows_to_generate INT NULL;

--changeset repo:2028-09-15-lead-portal-flow-metadata dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'model'
ALTER TABLE lead_portal_flow
    ADD COLUMN model VARCHAR(128) NULL,
    ADD COLUMN prompt LONGTEXT NULL,
    ADD COLUMN approved TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN approved_at TIMESTAMP NULL;
