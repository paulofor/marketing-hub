--liquibase formatted sql
--changeset repo:2030-01-05-flow-submission-image-watermarks dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_item'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_watermark'
CREATE TABLE flow_submission_image_watermark (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_flow_submission_image_watermark_item UNIQUE (item_id),
    CONSTRAINT fk_flow_submission_image_watermark_item FOREIGN KEY (item_id) REFERENCES flow_submission_image_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_flow_submission_image_watermark_asset FOREIGN KEY (asset_id) REFERENCES asset(id)
) ENGINE=InnoDB;
