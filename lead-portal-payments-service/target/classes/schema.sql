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

ALTER TABLE lead_portal_purchase
    ADD COLUMN IF NOT EXISTS mp_payment_payload LONGTEXT AFTER notification_payload;

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
