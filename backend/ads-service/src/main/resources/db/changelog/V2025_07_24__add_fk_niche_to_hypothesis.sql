-- liquibase formatted sql
-- changeset marketinghub:2025-07-24-add-fk-niche-to-hypothesis
-- preconditions onFail:MARK_RAN
--    <not>
--        <columnExists tableName="hypothesis" columnName="market_niche_id"/>
--    </not>
ALTER TABLE hypothesis ADD COLUMN market_niche_id BIGINT NOT NULL;
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND CONSTRAINT_NAME = 'fk_hypothesis_niche' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hypothesis' AND INDEX_NAME = 'fk_hypothesis_niche';
ALTER TABLE hypothesis
    ADD CONSTRAINT fk_hypothesis_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);
