--liquibase formatted sql
--changeset repo:2034-03-15-lead-portal-flow-market-niche dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'market_niche_id'
ALTER TABLE lead_portal_flow
    ADD COLUMN market_niche_id BIGINT NULL AFTER experiment_id,
    ADD CONSTRAINT fk_lead_portal_flow_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);

--changeset repo:2034-03-15-lead-portal-flow-market-niche-backfill dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND column_name = 'market_niche_id'
UPDATE lead_portal_flow flow
    JOIN experiment exp ON flow.experiment_id = exp.id
SET flow.market_niche_id = exp.niche_id
WHERE flow.market_niche_id IS NULL;

--changeset repo:2034-03-15-lead-portal-flow-market-niche-index dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'lead_portal_flow' AND index_name = 'idx_lead_portal_flow_market_niche'
CREATE INDEX idx_lead_portal_flow_market_niche ON lead_portal_flow(market_niche_id);
