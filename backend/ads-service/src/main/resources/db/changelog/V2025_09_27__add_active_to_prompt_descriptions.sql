ALTER TABLE prompt_entity_description ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE prompt_attribute_description ADD COLUMN active BOOLEAN DEFAULT TRUE;
UPDATE prompt_entity_description SET active = TRUE WHERE active IS NULL;
UPDATE prompt_attribute_description SET active = TRUE WHERE active IS NULL;
