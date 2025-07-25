-- liquibase formatted sql
-- changeset marketinghub:2025-07-24-add-fk-niche-to-hypothesis
ALTER TABLE hypothesis ADD COLUMN market_niche_id BIGINT NOT NULL;
ALTER TABLE hypothesis
    ADD CONSTRAINT fk_hypothesis_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);
