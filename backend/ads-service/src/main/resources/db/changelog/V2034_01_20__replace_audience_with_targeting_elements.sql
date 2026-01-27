--liquibase formatted sql
--changeset repo:2034-01-20-drop-audience-tables dbms:mysql
DROP TABLE IF EXISTS audience_targeting_seed;
DROP TABLE IF EXISTS audience;

--changeset repo:2034-01-20-update-market-niche-targeting dbms:mysql
ALTER TABLE market_niche
    DROP COLUMN IF EXISTS audiences_to_generate,
    DROP COLUMN IF EXISTS audience_model,
    ADD COLUMN IF NOT EXISTS interests_to_generate INT,
    ADD COLUMN IF NOT EXISTS job_titles_to_generate INT,
    ADD COLUMN IF NOT EXISTS behaviors_to_generate INT,
    ADD COLUMN IF NOT EXISTS interest_model VARCHAR(191),
    ADD COLUMN IF NOT EXISTS job_title_model VARCHAR(191),
    ADD COLUMN IF NOT EXISTS behavior_model VARCHAR(191);

--changeset repo:2034-01-20-create-targeting-element dbms:mysql
CREATE TABLE IF NOT EXISTS targeting_element (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_niche_id BIGINT NOT NULL,
    hypothesis_id BINARY(16) NULL,
    type VARCHAR(32) NOT NULL,
    term VARCHAR(255) NOT NULL,
    description LONGTEXT NULL,
    prompt LONGTEXT NULL,
    model VARCHAR(191) NULL,
    source VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL,
    notes LONGTEXT NULL,
    last_reviewed_by VARCHAR(191) NULL,
    meta_id VARCHAR(100) NULL,
    meta_key VARCHAR(191) NULL,
    confidence DECIMAL(10,4) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_targeting_element_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id),
    CONSTRAINT fk_targeting_element_hypothesis FOREIGN KEY (hypothesis_id) REFERENCES hypothesis(id)
) ENGINE=InnoDB;

CREATE INDEX idx_targeting_element_niche_type_status
    ON targeting_element (market_niche_id, type, status);
CREATE INDEX idx_targeting_element_hypothesis
    ON targeting_element (hypothesis_id);
