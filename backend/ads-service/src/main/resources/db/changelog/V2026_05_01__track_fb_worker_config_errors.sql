--liquibase formatted sql
--changeset repo:2026-05-20-track-fb-worker-config-errors dbms:mysql
ALTER TABLE fb_account
    ADD COLUMN worker_last_validation_at datetime NULL,
    ADD COLUMN worker_last_validation_error_code varchar(128) NULL,
    ADD COLUMN worker_last_validation_error_detail varchar(512) NULL;
