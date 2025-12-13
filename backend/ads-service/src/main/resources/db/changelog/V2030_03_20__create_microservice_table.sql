--liquibase formatted sql
--changeset repo:2030-03-20-create-microservice-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'microservice';
CREATE TABLE microservice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    base_url VARCHAR(512),
    category VARCHAR(100),
    status VARCHAR(50),
    owner VARCHAR(255),
    documentation_url VARCHAR(512),
    health_check_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
