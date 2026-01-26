--liquibase formatted sql
--changeset repo:2032-04-01-audience-targeting-spec-columns dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'audience' AND column_name = 'targeting_spec';
-- Estrutura para guardar Targeting Spec e seeds normalizadas
ALTER TABLE audience
    ADD COLUMN targeting_spec LONGTEXT NULL,
    ADD COLUMN targeting_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN targeting_notes LONGTEXT NULL,
    ADD COLUMN source VARCHAR(50) NULL,
    ADD COLUMN last_reviewed_by VARCHAR(255) NULL;

--changeset repo:2032-04-01-audience-targeting-seed-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema = DATABASE() AND table_name = 'audience_targeting_seed';
CREATE TABLE audience_targeting_seed (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audience_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    value VARCHAR(500) NOT NULL,
    meta_id VARCHAR(100),
    `key` VARCHAR(200),
    confidence DECIMAL(10,4),
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_audience_seed_audience FOREIGN KEY (audience_id) REFERENCES audience(id) ON DELETE CASCADE
);

--changeset repo:2032-04-01-audience-targeting-status-backfill dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'audience' AND column_name = 'targeting_status';
UPDATE audience SET targeting_status = 'DRAFT' WHERE targeting_status IS NULL;
