--liquibase formatted sql
--changeset repo:2025-02-12-journey-step-stimulus-type dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'journey_step' AND column_name = 'stimulus_type' AND (character_maximum_length IS NULL OR character_maximum_length >= 255)
ALTER TABLE journey_step MODIFY stimulus_type VARCHAR(255) NULL;
