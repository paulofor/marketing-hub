--liquibase formatted sql
--changeset repo:2037-04-06-add-image-filter-title-to-hypothesis dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hypothesis' AND column_name = 'image_filter_title';
ALTER TABLE hypothesis ADD COLUMN image_filter_title VARCHAR(255) NULL;
