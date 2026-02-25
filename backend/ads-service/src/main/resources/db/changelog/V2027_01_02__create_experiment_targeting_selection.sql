--liquibase formatted sql

--changeset repo:2027-01-02-create-experiment-targeting-selection dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'experiment_targeting_selection';
CREATE TABLE experiment_targeting_selection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    candidate_type VARCHAR(32) NOT NULL,
    term VARCHAR(191) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_experiment_targeting_selection_experiment
        FOREIGN KEY (experiment_id) REFERENCES experiment(id) ON DELETE CASCADE,
    INDEX idx_experiment_targeting_selection_experiment_id (experiment_id)
);
