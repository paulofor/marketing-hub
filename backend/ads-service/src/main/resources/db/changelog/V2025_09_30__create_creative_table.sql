-- liquibase formatted sql
-- changeset marketinghub:2025-09-30-create-creative-table
-- preconditions onFail:MARK_RAN
--    <not>
--        <tableExists tableName="creative"/>
--    </not>
CREATE TABLE creative (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    headline VARCHAR(255),
    primary_text VARCHAR(255),
    image_url VARCHAR(500),
    image_hash VARCHAR(255),
    video_id VARCHAR(255),
    status VARCHAR(20),
    CONSTRAINT fk_creative_experiment_id FOREIGN KEY (experiment_id) REFERENCES experiment(id)
);
