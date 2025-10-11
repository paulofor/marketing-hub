--liquibase formatted sql
--changeset marketinghub:2026-10-20-add-approval-to-fb-instant-form dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'fb_instant_form' AND column_name = 'approved';
ALTER TABLE fb_instant_form
    ADD COLUMN approved TINYINT(1) NOT NULL DEFAULT 0 AFTER prompt,
    ADD COLUMN approved_at DATETIME NULL AFTER approved;
