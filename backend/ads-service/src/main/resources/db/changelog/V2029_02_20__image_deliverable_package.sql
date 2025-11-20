--liquibase formatted sql
--changeset repo:2029-02-20-image-deliverable-packages dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'image_deliverable_package'
CREATE TABLE image_deliverable_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id BINARY(16) NOT NULL,
    input_asset_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    planned_outputs INT NULL,
    free_images INT NOT NULL DEFAULT 0,
    model VARCHAR(255),
    prompt LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_package_lead FOREIGN KEY (lead_id) REFERENCES `lead`(id),
    CONSTRAINT fk_image_package_asset FOREIGN KEY (input_asset_id) REFERENCES asset(id)
);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'image_deliverable_item'
CREATE TABLE image_deliverable_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    access_type VARCHAR(20) NOT NULL,
    position_index INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_image_item_package FOREIGN KEY (package_id) REFERENCES image_deliverable_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_image_item_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    CONSTRAINT uq_image_deliverable_item_order UNIQUE KEY (package_id, position_index)
);

CREATE INDEX idx_image_package_lead ON image_deliverable_package(lead_id, created_at DESC);
CREATE INDEX idx_image_item_package ON image_deliverable_item(package_id);
