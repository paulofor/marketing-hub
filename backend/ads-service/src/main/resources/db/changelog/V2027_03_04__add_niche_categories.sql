--liquibase formatted sql
--changeset marketinghub:2027-03-04-add-niche-categories dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'interest_category';
ALTER TABLE market_niche ADD COLUMN interest_category VARCHAR(255) NULL;

--changeset marketinghub:2027-03-04-add-role-category dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'role_category';
ALTER TABLE market_niche ADD COLUMN role_category VARCHAR(255) NULL;
