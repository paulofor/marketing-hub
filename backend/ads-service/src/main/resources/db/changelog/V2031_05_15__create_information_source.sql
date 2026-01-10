--liquibase formatted sql
--changeset marketinghub:2031-05-15-create-information-source dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'information_source';
CREATE TABLE information_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_niche_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_information_source_niche (market_niche_id),
    CONSTRAINT fk_information_source_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id)
);
