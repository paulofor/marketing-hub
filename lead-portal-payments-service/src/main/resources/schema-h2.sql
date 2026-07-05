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
    checkout_accessed_at TIMESTAMP NULL,
    amount DECIMAL(12,2),
    currency VARCHAR(8),
    notification_payload CLOB,
    mp_payment_payload CLOB,
    delivery_attempts INT DEFAULT 0,
    delivery_error CLOB,
    delivered_at TIMESTAMP NULL,
    payment_approved_at TIMESTAMP NULL,
    pixel_conversion_recorded_at TIMESTAMP NULL,
    zip_object_key VARCHAR(512),
    zip_size_bytes BIGINT,
    zip_generated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lead_portal_purchase_package ON lead_portal_purchase(package_id);
CREATE INDEX IF NOT EXISTS idx_lead_portal_purchase_payment ON lead_portal_purchase(mp_payment_id);
CREATE INDEX IF NOT EXISTS idx_lead_portal_purchase_checkout_accessed_at ON lead_portal_purchase(checkout_accessed_at);

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
    zip_last_error CLOB,
    email_request_id VARCHAR(64),
    email_sent_at TIMESTAMP NULL,
    email_attempts INT NOT NULL DEFAULT 0,
    email_last_attempt TIMESTAMP NULL,
    email_last_error CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_premium_delivery_purchase FOREIGN KEY (purchase_id) REFERENCES lead_portal_purchase(id),
    CONSTRAINT uq_premium_delivery_purchase UNIQUE (purchase_id)
);

CREATE INDEX IF NOT EXISTS idx_premium_delivery_status ON lead_portal_premium_delivery(status, updated_at);

CREATE TABLE IF NOT EXISTS mercadopago_webhook_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id VARCHAR(150),
    topic VARCHAR(100),
    query_id VARCHAR(150),
    query_topic VARCHAR(100),
    payload_type VARCHAR(100),
    payload_action VARCHAR(100),
    has_payload BOOLEAN,
    payload CLOB,
    mercadopago_status VARCHAR(80),
    mercadopago_response CLOB,
    processing_status VARCHAR(40) NOT NULL,
    error_message CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mp_webhook_resource ON mercadopago_webhook_log(resource_id);
CREATE INDEX IF NOT EXISTS idx_mp_webhook_created_at ON mercadopago_webhook_log(created_at);

CREATE TABLE IF NOT EXISTS digital_product_delivery_email (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(150) NOT NULL UNIQUE,
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
    last_error CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_digital_product_delivery_status
    ON digital_product_delivery_email(status, updated_at);
