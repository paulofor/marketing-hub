CREATE TABLE hypothesis (
    id BINARY(16) PRIMARY KEY,
    experiment_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    premise_angle_id BIGINT NOT NULL,
    offer_type VARCHAR(20) NOT NULL,
    price DECIMAL(6,2),
    kpi_target_cpl DECIMAL(7,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'BACKLOG' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
