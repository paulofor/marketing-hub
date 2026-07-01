CREATE TABLE IF NOT EXISTS lead_portal_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    submission_id VARCHAR(64),
    buyer_name VARCHAR(255),
    buyer_email VARCHAR(320),
    status VARCHAR(40) NOT NULL,
    mp_preference_id VARCHAR(150),
    mp_payment_id VARCHAR(150),
    mp_status VARCHAR(80),
    checkout_url VARCHAR(1200),
    checkout_expires_at TIMESTAMP NULL,
    amount DECIMAL(12,2),
    currency VARCHAR(8),
    notification_payload LONGTEXT,
    mp_payment_payload LONGTEXT,
    delivery_attempts INT DEFAULT 0,
    delivery_error LONGTEXT,
    delivered_at TIMESTAMP NULL,
    payment_approved_at TIMESTAMP NULL,
    zip_object_key VARCHAR(512),
    zip_size_bytes BIGINT,
    zip_generated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_lead_portal_purchase_package (package_id),
    INDEX idx_lead_portal_purchase_payment (mp_payment_id)
);

SET @mp_payment_payload_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lead_portal_purchase'
      AND column_name = 'mp_payment_payload'
);

SET @add_mp_payment_payload_column = IF(
    @mp_payment_payload_exists = 0,
    'ALTER TABLE lead_portal_purchase ADD COLUMN mp_payment_payload LONGTEXT AFTER notification_payload',
    'SELECT 1'
);

PREPARE add_mp_payment_payload_stmt FROM @add_mp_payment_payload_column;
EXECUTE add_mp_payment_payload_stmt;
DEALLOCATE PREPARE add_mp_payment_payload_stmt;


CREATE TABLE IF NOT EXISTS lead_portal_premium_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    submission_id VARCHAR(36),
    submission_name VARCHAR(255),
    submission_email VARCHAR(320),
    buyer_name VARCHAR(255),
    buyer_email VARCHAR(320),
    recipient_name VARCHAR(255),
    recipient_email VARCHAR(320) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ZIP',
    zip_object_key VARCHAR(512),
    zip_download_url VARCHAR(1024),
    zip_size_bytes BIGINT,
    zip_generated_at TIMESTAMP NULL,
    zip_attempts INT NOT NULL DEFAULT 0,
    zip_last_attempt TIMESTAMP NULL,
    zip_last_error TEXT NULL,
    email_request_id VARCHAR(64),
    email_sent_at TIMESTAMP NULL,
    email_attempts INT NOT NULL DEFAULT 0,
    email_last_attempt TIMESTAMP NULL,
    email_last_error TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_premium_delivery_purchase FOREIGN KEY (purchase_id) REFERENCES lead_portal_purchase(id)
);
SET @premium_delivery_unique_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lead_portal_premium_delivery'
      AND index_name = 'uq_premium_delivery_purchase'
);

SET @create_premium_delivery_unique_idx = IF(
    @premium_delivery_unique_idx_exists = 0,
    'CREATE UNIQUE INDEX uq_premium_delivery_purchase ON lead_portal_premium_delivery(purchase_id)',
    'SELECT 1'
);

PREPARE create_premium_delivery_unique_idx_stmt FROM @create_premium_delivery_unique_idx;
EXECUTE create_premium_delivery_unique_idx_stmt;
DEALLOCATE PREPARE create_premium_delivery_unique_idx_stmt;

SET @premium_delivery_status_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lead_portal_premium_delivery'
      AND index_name = 'idx_premium_delivery_status'
);

SET @create_premium_delivery_status_idx = IF(
    @premium_delivery_status_idx_exists = 0,
    'CREATE INDEX idx_premium_delivery_status ON lead_portal_premium_delivery(status, updated_at)',
    'SELECT 1'
);

PREPARE create_premium_delivery_status_idx_stmt FROM @create_premium_delivery_status_idx;
EXECUTE create_premium_delivery_status_idx_stmt;
DEALLOCATE PREPARE create_premium_delivery_status_idx_stmt;

CREATE TABLE IF NOT EXISTS mercadopago_webhook_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id VARCHAR(150),
    topic VARCHAR(100),
    query_id VARCHAR(150),
    query_topic VARCHAR(100),
    payload_type VARCHAR(100),
    payload_action VARCHAR(100),
    has_payload BOOLEAN,
    payload LONGTEXT,
    mercadopago_status VARCHAR(80),
    mercadopago_response LONGTEXT,
    processing_status VARCHAR(40) NOT NULL,
    error_message LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mp_webhook_resource (resource_id),
    INDEX idx_mp_webhook_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS digital_product_delivery_email (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(150) NOT NULL,
    external_reference VARCHAR(150) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    recipient_name VARCHAR(255),
    product_name VARCHAR(255) NOT NULL,
    delivery_page_url VARCHAR(1200),
    download_url VARCHAR(1200),
    status VARCHAR(30) NOT NULL,
    email_request_id VARCHAR(64),
    sent_at TIMESTAMP NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_digital_product_delivery_payment (payment_id),
    INDEX idx_digital_product_delivery_status (status, updated_at)
);
