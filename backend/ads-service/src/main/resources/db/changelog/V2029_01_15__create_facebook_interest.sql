--liquibase formatted sql
--changeset marketinghub:2029-01-15-create-facebook-interest dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'facebook_interest';
CREATE TABLE facebook_interest (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  facebook_interest_id VARCHAR(128),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  model VARCHAR(128),
  prompt LONGTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
