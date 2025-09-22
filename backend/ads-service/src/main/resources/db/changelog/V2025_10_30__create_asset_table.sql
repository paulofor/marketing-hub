--liquibase formatted sql

--changeset marketinghub:2025-10-30-create-asset-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'asset'
CREATE TABLE asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255),
    provider VARCHAR(255),
    external_id VARCHAR(255),
    status VARCHAR(255),
    url VARCHAR(1024),
    payload LONGTEXT,
    campaign_id BIGINT,
    model VARCHAR(255),
    prompt LONGTEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--changeset marketinghub:2025-10-30-add-asset-model-column dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'model'
ALTER TABLE asset ADD COLUMN model VARCHAR(255);

--changeset marketinghub:2025-10-30-add-asset-prompt-column dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asset' AND column_name = 'prompt'
ALTER TABLE asset ADD COLUMN prompt LONGTEXT;
