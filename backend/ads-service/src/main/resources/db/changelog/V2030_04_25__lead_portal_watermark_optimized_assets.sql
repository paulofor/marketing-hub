--liquibase formatted sql
--changeset repo:2030-04-25-lead-portal-watermark-optimized-assets dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_watermark'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_watermark' AND column_name = 'optimized_asset_id'
ALTER TABLE flow_submission_image_watermark
    ADD COLUMN optimized_asset_id BIGINT NULL AFTER asset_id,
    ADD CONSTRAINT fk_flow_submission_image_watermark_opt_asset FOREIGN KEY (optimized_asset_id) REFERENCES asset(id);

CREATE INDEX idx_flow_submission_image_watermark_opt_asset ON flow_submission_image_watermark (optimized_asset_id);
