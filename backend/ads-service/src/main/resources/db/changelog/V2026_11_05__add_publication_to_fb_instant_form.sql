--liquibase formatted sql
--changeset repo:2026-11-05-add-publication-to-fb-instant-form dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'fb_instant_form' AND column_name = 'published';
ALTER TABLE fb_instant_form
    ADD COLUMN published TINYINT(1) NOT NULL DEFAULT 0 AFTER approved_at,
    ADD COLUMN published_at DATETIME NULL AFTER published,
    ADD COLUMN share_link VARCHAR(512) NULL AFTER published_at;
