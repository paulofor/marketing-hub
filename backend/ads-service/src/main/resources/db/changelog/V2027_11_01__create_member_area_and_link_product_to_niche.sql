--liquibase formatted sql
--changeset repo:2027-11-01-product-member-area dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'market_niche_id';
ALTER TABLE product ADD COLUMN market_niche_id BIGINT AFTER instagram_account_id;

--changeset repo:2027-11-01-product-member-area-fk dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'product' AND constraint_name = 'fk_product_market_niche';
ALTER TABLE product ADD CONSTRAINT fk_product_market_niche FOREIGN KEY (market_niche_id) REFERENCES market_niche(id);

--changeset repo:2027-11-01-member-area-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'member_area';
CREATE TABLE member_area (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(255),
    access_url VARCHAR(500),
    description LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_area_product FOREIGN KEY (product_id) REFERENCES product(id)
);
