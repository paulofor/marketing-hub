--liquibase formatted sql
--changeset repo:2034-01-22-drop-audience-targeting-seed-fix dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'audience_targeting_seed';
DROP TABLE IF EXISTS audience_targeting_seed;

--changeset repo:2034-01-22-drop-audience-table-fix dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'audience';
DROP TABLE IF EXISTS audience;
