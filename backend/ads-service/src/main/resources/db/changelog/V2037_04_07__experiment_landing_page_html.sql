--liquibase formatted sql
--changeset repo:2037-04-07-experiment-landing-page-html dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'landing_page_html';
ALTER TABLE experiment
    ADD COLUMN landing_page_html LONGTEXT NULL AFTER landing_page_wireframe;
