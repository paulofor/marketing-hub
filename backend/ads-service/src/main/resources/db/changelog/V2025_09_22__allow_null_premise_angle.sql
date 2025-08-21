-- Allows hypotheses to be created without an angle
-- liquibase formatted sql
-- changeset marketinghub:2025-09-22-allow-null-premise-angle
ALTER TABLE hypothesis MODIFY premise_angle_id BIGINT NULL;
