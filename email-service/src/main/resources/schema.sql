CREATE TABLE IF NOT EXISTS email_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL UNIQUE,
    recipients TEXT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    opened_at TIMESTAMP NULL
);
