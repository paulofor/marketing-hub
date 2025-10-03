--liquibase formatted sql
--changeset repo:2026-05-20-add-facebook-page-ref dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'facebook_page_id';
ALTER TABLE experiment
    ADD COLUMN facebook_page_id BIGINT NULL,
    ADD CONSTRAINT fk_experiment_facebook_page FOREIGN KEY (facebook_page_id) REFERENCES fb_page(id);

--changeset repo:2026-05-20-migrate-facebook-page-ref dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'page_id';
UPDATE experiment e
JOIN fb_page p ON p.page_id = e.page_id
SET e.facebook_page_id = p.id
WHERE e.page_id IS NOT NULL AND e.page_id <> '';

--changeset repo:2026-05-20-drop-experiment-page-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'page_id';
ALTER TABLE experiment DROP COLUMN page_id;
