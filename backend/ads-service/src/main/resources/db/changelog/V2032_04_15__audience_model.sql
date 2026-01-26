ALTER TABLE market_niche
    ADD COLUMN audience_model VARCHAR(191);

UPDATE market_niche
SET audience_model = hypothesis_model
WHERE audience_model IS NULL
  AND hypothesis_model IS NOT NULL;
