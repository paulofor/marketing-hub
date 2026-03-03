--liquibase formatted sql
--changeset repo:2036-03-03-lead-portal-flow-description-longtext dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'description' AND data_type <> 'longtext';
ALTER TABLE lead_portal_flow
    MODIFY COLUMN description LONGTEXT NULL;
