--liquibase formatted sql
--changeset repo:2032-04-15-1 dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'audience_model';
ALTER TABLE market_niche
    ADD COLUMN audience_model VARCHAR(191);

--changeset repo:2032-04-15-2 dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'audience_model';
UPDATE market_niche
SET audience_model = hypothesis_model
WHERE audience_model IS NULL
  AND hypothesis_model IS NOT NULL;
