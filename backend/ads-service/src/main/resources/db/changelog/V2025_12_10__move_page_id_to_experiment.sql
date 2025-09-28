--liquibase formatted sql
--changeset repo:2025-12-10-add-page-id-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'experiment' AND COLUMN_NAME = 'page_id';
ALTER TABLE experiment ADD COLUMN page_id VARCHAR(128);

--changeset repo:2025-12-10-migrate-page-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'creative' AND COLUMN_NAME = 'page_id';
UPDATE experiment e
JOIN (
    SELECT experiment_id, MAX(page_id) AS page_id
    FROM creative
    WHERE page_id IS NOT NULL AND page_id <> ''
    GROUP BY experiment_id
) src ON src.experiment_id = e.id
SET e.page_id = src.page_id
WHERE src.page_id IS NOT NULL;

--changeset repo:2025-12-10-drop-page-id-from-creative dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'creative' AND COLUMN_NAME = 'page_id';
ALTER TABLE creative DROP COLUMN page_id;
