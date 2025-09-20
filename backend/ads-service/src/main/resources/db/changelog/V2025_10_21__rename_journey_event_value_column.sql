--liquibase formatted sql

--changeset marketinghub:2025-10-21-rename-journey-event-value-column
--preconditions onFail=MARK_RAN onError=HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'journey_event_log' AND column_name = 'value';
ALTER TABLE journey_event_log CHANGE value event_value DECIMAL(12,2);
