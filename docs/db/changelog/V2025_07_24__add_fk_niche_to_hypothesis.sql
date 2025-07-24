ALTER TABLE hypothesis ADD COLUMN market_niche_id BIGINT NOT NULL;
ALTER TABLE hypothesis ADD CONSTRAINT fk_hypothesis_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);
