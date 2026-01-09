--liquibase formatted sql
--changeset marketinghub:2031-02-20-add-min-max-quantities dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'interaction_journey_element' AND column_name IN ('min_quantity','max_quantity');
ALTER TABLE interaction_journey_element
    ADD COLUMN min_quantity INT NULL,
    ADD COLUMN max_quantity INT NULL;
