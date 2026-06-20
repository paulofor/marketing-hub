--liquibase formatted sql
--changeset marketinghub:add-capture-destination-to-experiment dbms:mysql splitStatements:true stripComments:true
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'capture_destination_type';
ALTER TABLE experiment
    ADD COLUMN capture_destination_type VARCHAR(32) NOT NULL DEFAULT 'LANDING_PAGE' AFTER follow_up_action_url;
