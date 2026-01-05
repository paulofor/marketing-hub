--liquibase formatted sql
--changeset repo:2030-09-15-openai-models dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'openai_model';
CREATE TABLE openai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(128) NOT NULL,
    price_input_standard DECIMAL(12,5) NOT NULL,
    price_input_cached_standard DECIMAL(12,5) NOT NULL,
    price_output_standard DECIMAL(12,5) NOT NULL,
    price_input_batch DECIMAL(12,5) NOT NULL,
    price_input_cached_batch DECIMAL(12,5) NOT NULL,
    price_output_batch DECIMAL(12,5) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_openai_model_code UNIQUE (code)
) ENGINE=InnoDB;
