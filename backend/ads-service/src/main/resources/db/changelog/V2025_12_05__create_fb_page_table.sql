--liquibase formatted sql
--changeset marketinghub:2025-12-05-create-fb-page-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'fb_page';
CREATE TABLE fb_page (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  page_id VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  CONSTRAINT fk_fb_page_account FOREIGN KEY (account_id) REFERENCES fb_account(id),
  CONSTRAINT uq_fb_page_account_page UNIQUE (account_id, page_id)
);
