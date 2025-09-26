--liquibase formatted sql
--changeset marketinghub:2025-11-26-extend-creative dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'creative' AND COLUMN_NAME IN ('ad_format','description','call_to_action','destination_url','page_id','instagram_user_id');
ALTER TABLE creative
    ADD COLUMN ad_format VARCHAR(32) NULL AFTER image_url,
    ADD COLUMN description VARCHAR(255) NULL AFTER ad_format,
    ADD COLUMN call_to_action VARCHAR(32) NULL AFTER description,
    ADD COLUMN destination_url VARCHAR(512) NULL AFTER call_to_action,
    ADD COLUMN page_id VARCHAR(64) NULL AFTER destination_url,
    ADD COLUMN instagram_user_id VARCHAR(64) NULL AFTER page_id;
