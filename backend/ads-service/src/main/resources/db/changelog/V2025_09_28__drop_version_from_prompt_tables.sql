-- liquibase formatted sql
-- changeset marketinghub:2025-09-28-drop-version-prompt_entity
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prompt_entity' AND COLUMN_NAME = 'version';
ALTER TABLE prompt_entity DROP COLUMN version;

-- changeset marketinghub:2025-09-28-drop-version-prompt_attribute
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prompt_attribute' AND COLUMN_NAME = 'version';
ALTER TABLE prompt_attribute DROP COLUMN version;

-- changeset marketinghub:2025-09-28-drop-version-prompt_entity_description
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prompt_entity_description' AND COLUMN_NAME = 'version';
ALTER TABLE prompt_entity_description DROP COLUMN version;

-- changeset marketinghub:2025-09-28-drop-version-prompt_attribute_description
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prompt_attribute_description' AND COLUMN_NAME = 'version';
ALTER TABLE prompt_attribute_description DROP COLUMN version;
