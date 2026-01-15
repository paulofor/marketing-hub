--liquibase formatted sql
--changeset repo:2031-09-20-add-active-detailed-description dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'niche_detailed_description' AND column_name = 'active';
ALTER TABLE niche_detailed_description
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1;

--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'hypothesis_detailed_description_id';
ALTER TABLE market_niche
    ADD COLUMN hypothesis_detailed_description_id BIGINT;

--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND constraint_name = 'fk_market_niche_hypothesis_detailed_description';
ALTER TABLE market_niche
    ADD CONSTRAINT fk_market_niche_hypothesis_detailed_description
        FOREIGN KEY (hypothesis_detailed_description_id)
            REFERENCES niche_detailed_description(id);
