--liquibase formatted sql

--changeset repo:2027-02-05-add-experiment-fk-to-targeting-request dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'targeting_request' AND COLUMN_NAME = 'experiment_id';
ALTER TABLE targeting_request
    ADD COLUMN experiment_id BIGINT NULL AFTER hypothesis_id,
    ADD CONSTRAINT fk_targeting_request_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id) ON DELETE SET NULL;
CREATE INDEX idx_targeting_request_experiment_id ON targeting_request (experiment_id);
