--liquibase formatted sql
--changeset marketinghub:2026-12-15-create-app-idea dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'app_idea';
CREATE TABLE app_idea (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    niche VARCHAR(255) NOT NULL,
    target_audience VARCHAR(255) NULL,
    problem_to_solve LONGTEXT NULL,
    value_proposition LONGTEXT NULL,
    core_features LONGTEXT NULL,
    differentiator LONGTEXT NULL,
    monetization LONGTEXT NULL,
    go_to_market LONGTEXT NULL,
    technology_stack LONGTEXT NULL,
    model VARCHAR(255) NULL,
    prompt LONGTEXT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);
