--liquibase formatted sql

--changeset repo:2026-12-31-add-market-niche-interest-list dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'interest_list';
ALTER TABLE market_niche ADD COLUMN interest_list LONGTEXT NULL;

--changeset repo:2026-12-31-add-market-niche-role-list dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'role_list';
ALTER TABLE market_niche ADD COLUMN role_list LONGTEXT NULL;

--changeset repo:2026-12-31-add-market-niche-behavior-list dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'market_niche' AND column_name = 'behavior_list';
ALTER TABLE market_niche ADD COLUMN behavior_list LONGTEXT NULL;
