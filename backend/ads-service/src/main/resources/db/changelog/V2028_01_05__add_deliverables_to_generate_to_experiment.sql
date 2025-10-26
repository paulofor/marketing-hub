--liquibase formatted sql
--changeset repo:2028-01-05-add-deliverables-to-generate-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'deliverables_to_generate';
ALTER TABLE experiment
    ADD COLUMN deliverables_to_generate INT NULL AFTER emails_to_generate;
