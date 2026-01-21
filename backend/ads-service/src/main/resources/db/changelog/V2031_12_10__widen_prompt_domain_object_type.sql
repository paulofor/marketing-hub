--liquibase formatted sql

--changeset repo:2031-12-10-widen-prompt-domain-object-type dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'prompt_domain_object';
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'prompt_domain_object' AND column_name = 'object_type' AND data_type = 'varchar' AND character_maximum_length >= 64;
ALTER TABLE prompt_domain_object
    MODIFY COLUMN object_type VARCHAR(64) NOT NULL;
