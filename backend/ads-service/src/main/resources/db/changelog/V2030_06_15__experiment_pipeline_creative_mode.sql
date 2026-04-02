-- liquibase formatted sql
-- changeset ai:experiment_creative_generation_mode
ALTER TABLE experiment
    ADD COLUMN creative_generation_mode VARCHAR(32) NOT NULL DEFAULT 'DEFAULT' AFTER stage;
