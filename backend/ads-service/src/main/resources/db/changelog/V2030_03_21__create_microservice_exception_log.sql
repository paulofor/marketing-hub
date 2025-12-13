--liquibase formatted sql
--changeset repo:2030-03-21-create-microservice-exception-log dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'microservice_exception_log';
CREATE TABLE microservice_exception_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    microservice_id BIGINT NOT NULL,
    exception_type VARCHAR(255),
    message LONGTEXT,
    stack_trace LONGTEXT,
    severity VARCHAR(20),
    service_version VARCHAR(100),
    hostname VARCHAR(255),
    context LONGTEXT,
    occurred_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_microservice_exception_microservice FOREIGN KEY (microservice_id) REFERENCES microservice(id)
);
CREATE INDEX idx_microservice_exception_microservice ON microservice_exception_log (microservice_id, occurred_at DESC);
CREATE INDEX idx_microservice_exception_severity ON microservice_exception_log (severity);
