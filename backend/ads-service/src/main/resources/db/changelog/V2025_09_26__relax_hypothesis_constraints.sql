-- liquibase formatted sql
-- changeset marketinghub:2025-09-26-relax-hypothesis-constraints
ALTER TABLE hypothesis MODIFY kpi_target_cpl DECIMAL(7,2) NULL;
ALTER TABLE hypothesis MODIFY offer_type VARCHAR(20) NULL;
ALTER TABLE hypothesis MODIFY status VARCHAR(20) NULL DEFAULT 'BACKLOG';
ALTER TABLE hypothesis MODIFY market_niche_id BIGINT NULL;
