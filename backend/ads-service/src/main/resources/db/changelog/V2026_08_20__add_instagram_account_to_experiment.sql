--liquibase formatted sql
--changeset marketinghub:2026-08-20-add-instagram-account-to-experiment dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'experiment' AND column_name = 'instagram_account_id';
ALTER TABLE experiment
  ADD COLUMN instagram_account_id BIGINT NULL;

ALTER TABLE experiment
  ADD CONSTRAINT fk_experiment_instagram_account FOREIGN KEY (instagram_account_id) REFERENCES ig_account(id);
