ALTER TABLE hypothesis
    MODIFY market_niche_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_hypothesis_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);
