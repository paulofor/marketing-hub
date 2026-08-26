CREATE TABLE pde_production_slot (
    id BIGINT NOT NULL PRIMARY KEY,
    product_slug VARCHAR(191) NOT NULL,
    source_experiment_id BIGINT NULL,
    experience_version VARCHAR(191) NOT NULL,
    layout_key VARCHAR(191) NOT NULL,
    status VARCHAR(32) NOT NULL,
    draft_experience_json LONGTEXT NULL,
    published_experience_json LONGTEXT NULL,
    published_by VARCHAR(191) NULL,
    published_at DATETIME NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO pde_production_slot
  (id, product_slug, source_experiment_id, experience_version, layout_key, status,
   draft_experience_json, published_experience_json, published_by, published_at, updated_at)
VALUES
  (7, 'kit-whatsapp-pronto', 89, 'kit-whatsapp-pronto-pde-v1', 'assisted-service-v1',
   'ACTIVE', NULL,
   '{"experienceVersion":"kit-whatsapp-pronto-pde-v1","layoutKey":"assisted-service-v1","funnelVersion":"pde-assisted-service-v1"}',
   'fixture-v1', '2026-08-22 21:55:06', '2026-08-22 21:55:42');
