--liquibase formatted sql
--changeset repo:2026-02-26-targeting-element-meta-bounds dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'targeting_element' AND column_name = 'meta_audience_size_lower_bound';
ALTER TABLE targeting_element
    ADD COLUMN meta_audience_size_lower_bound BIGINT NULL,
    ADD COLUMN meta_audience_size_upper_bound BIGINT NULL;
