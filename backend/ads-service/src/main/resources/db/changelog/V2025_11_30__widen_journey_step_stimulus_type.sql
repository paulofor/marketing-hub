--liquibase formatted sql
--changeset repo:2025-11-19-widen-journey-stimulus-type dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'journey_step' AND column_name = 'stimulus_type' AND data_type = 'varchar' AND character_maximum_length >= 128;
ALTER TABLE journey_step MODIFY COLUMN stimulus_type VARCHAR(128) NOT NULL;
