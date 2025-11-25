--liquibase formatted sql
--changeset repo:2029-03-15-flow-submission-image-packages dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flow_submissions'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_package'
CREATE TABLE flow_submission_image_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    planned_outputs INT NULL,
    free_images INT NOT NULL DEFAULT 0,
    model VARCHAR(255),
    prompt LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_package_submission FOREIGN KEY (submission_id) REFERENCES flow_submissions(id)
) ENGINE=InnoDB;

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flow_submission_image_item'
CREATE TABLE flow_submission_image_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    access_type VARCHAR(20) NOT NULL,
    position_index INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flow_submission_image_item_package FOREIGN KEY (package_id) REFERENCES flow_submission_image_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_flow_submission_image_item_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    CONSTRAINT uq_flow_submission_image_item_order UNIQUE KEY (package_id, position_index)
) ENGINE=InnoDB;

CREATE INDEX idx_flow_submission_image_package_submission ON flow_submission_image_package(submission_id, created_at DESC);
CREATE INDEX idx_flow_submission_image_item_package ON flow_submission_image_item(package_id);
