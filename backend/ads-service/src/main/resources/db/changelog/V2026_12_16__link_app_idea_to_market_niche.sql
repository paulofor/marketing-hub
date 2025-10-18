--liquibase formatted sql
--changeset marketinghub:2026-12-16-link-app-idea-to-market-niche dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_idea' AND column_name = 'market_niche_id';
ALTER TABLE app_idea
    ADD COLUMN market_niche_id BIGINT NULL;

INSERT INTO market_niche (name)
SELECT DISTINCT ai.niche
FROM app_idea ai
LEFT JOIN market_niche mn ON mn.name = ai.niche
WHERE ai.niche IS NOT NULL AND TRIM(ai.niche) <> '' AND mn.id IS NULL;

UPDATE app_idea ai
JOIN market_niche mn ON mn.name = ai.niche
SET ai.market_niche_id = mn.id
WHERE ai.market_niche_id IS NULL;

ALTER TABLE app_idea
    MODIFY market_niche_id BIGINT NOT NULL;

ALTER TABLE app_idea
    ADD CONSTRAINT fk_app_idea_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);

ALTER TABLE app_idea
    DROP COLUMN niche;
