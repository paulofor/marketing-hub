--liquibase formatted sql
--changeset repo:2027-12-22-create-deliverables dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'deliverable';
CREATE TABLE deliverable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_niche_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    content LONGTEXT,
    model VARCHAR(255),
    prompt LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_deliverable_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id)
);

--changeset repo:2027-12-22-create-deliverable-packages dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'deliverable_package';
CREATE TABLE deliverable_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    model VARCHAR(255),
    prompt LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_deliverable_package_experiment_name UNIQUE (experiment_id, name),
    CONSTRAINT fk_deliverable_package_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(id)
);

--changeset repo:2027-12-22-create-deliverable-package-items dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'deliverable_package_item';
CREATE TABLE deliverable_package_item (
    deliverable_package_id BIGINT NOT NULL,
    deliverable_id BIGINT NOT NULL,
    PRIMARY KEY (deliverable_package_id, deliverable_id),
    CONSTRAINT fk_package_item_package FOREIGN KEY (deliverable_package_id) REFERENCES deliverable_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_package_item_deliverable FOREIGN KEY (deliverable_id) REFERENCES deliverable(id) ON DELETE CASCADE
);

CREATE INDEX idx_deliverable_niche ON deliverable(market_niche_id);
CREATE INDEX idx_deliverable_package_experiment ON deliverable_package(experiment_id);
