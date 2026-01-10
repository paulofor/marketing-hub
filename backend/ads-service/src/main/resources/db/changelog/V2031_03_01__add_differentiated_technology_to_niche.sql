--liquibase formatted sql
--changeset marketinghub:2031-03-01-add-differentiated-technology-to-niche dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'differentiated_technology_id';
ALTER TABLE market_niche ADD COLUMN differentiated_technology_id BIGINT NULL;

--changeset marketinghub:2031-03-01-add-fk-market-niche-differentiated-technology dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND constraint_name = 'fk_market_niche_differentiated_technology';
ALTER TABLE market_niche
    ADD CONSTRAINT fk_market_niche_differentiated_technology
        FOREIGN KEY (differentiated_technology_id) REFERENCES differentiated_technology(id);
