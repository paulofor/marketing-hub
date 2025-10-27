--liquibase formatted sql
--changeset repo:2028-02-01-create-whatsapp-account dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'whatsapp_account';
CREATE TABLE whatsapp_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30),
    phone_number_id VARCHAR(64) NOT NULL,
    business_account_id VARCHAR(64),
    access_token LONGTEXT,
    verify_token VARCHAR(255),
    base_url VARCHAR(255),
    active TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

--changeset repo:2028-02-01-create-whatsapp-message dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'whatsapp_message';
CREATE TABLE whatsapp_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    message_type VARCHAR(30),
    message_id VARCHAR(128),
    from_number VARCHAR(30),
    to_number VARCHAR(30),
    status VARCHAR(50),
    error_code VARCHAR(50),
    error_message LONGTEXT,
    text_body LONGTEXT,
    image_url LONGTEXT,
    image_id VARCHAR(128),
    mime_type VARCHAR(100),
    caption LONGTEXT,
    conversation_id VARCHAR(128),
    context_json LONGTEXT,
    payload_json LONGTEXT,
    status_payload_json LONGTEXT,
    message_timestamp TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    received_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_whatsapp_message_account FOREIGN KEY (account_id) REFERENCES whatsapp_account (id)
) ENGINE=InnoDB;

CREATE UNIQUE INDEX ux_whatsapp_account_phone_number_id ON whatsapp_account (phone_number_id);
CREATE INDEX idx_whatsapp_message_message_id ON whatsapp_message (message_id);
CREATE INDEX idx_whatsapp_message_account ON whatsapp_message (account_id);
CREATE INDEX idx_whatsapp_message_direction ON whatsapp_message (direction);
