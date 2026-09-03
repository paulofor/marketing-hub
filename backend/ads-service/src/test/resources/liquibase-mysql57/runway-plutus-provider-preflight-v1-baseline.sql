CREATE TABLE sales_video_provider_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(191) NOT NULL,
    adapter_key VARCHAR(80) NOT NULL,
    external_model_id VARCHAR(191) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_video_provider_model_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sales_video_provider_model
    (code, display_name, adapter_key, external_model_id)
VALUES
    ('RUNWAY_SEEDANCE_2_5', 'Seedance 2.5 via Runway', 'RUNWAY', 'seedance2_5'),
    ('RUNWAY_VEO_3_1', 'Veo 3.1 via Runway', 'RUNWAY', 'veo3.1'),
    ('LUMA_RAY_2', 'Luma Ray 2 direto', 'LUMA', 'ray-2');

CREATE TABLE video_production_cycle (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO video_production_cycle (id, status, created_at, updated_at)
VALUES (31, 'PENDING_PROVIDER_PREFLIGHT', '2026-09-03 10:00:00', '2026-09-03 10:00:00');
