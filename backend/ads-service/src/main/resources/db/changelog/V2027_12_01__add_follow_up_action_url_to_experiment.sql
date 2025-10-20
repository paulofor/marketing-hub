--liquibase formatted sql
--changeset repo:2027-12-01-add-follow-up-action-url-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'follow_up_action_url';
ALTER TABLE experiment
    ADD COLUMN follow_up_action_url VARCHAR(512);
