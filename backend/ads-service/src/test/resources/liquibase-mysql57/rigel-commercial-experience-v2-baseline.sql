CREATE TABLE product (
    id BIGINT NOT NULL PRIMARY KEY,
    slug VARCHAR(191) NOT NULL UNIQUE,
    pde_experience_json LONGTEXT NULL,
    primary_cta VARCHAR(191) NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE experiment (
    id BIGINT NOT NULL PRIMARY KEY,
    funnel_promise VARCHAR(1000) NULL,
    primary_cta VARCHAR(191) NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pde_production_slot (
    id BIGINT NOT NULL PRIMARY KEY,
    product_slug VARCHAR(191) NOT NULL,
    source_experiment_id BIGINT NULL,
    experience_version VARCHAR(191) NOT NULL,
    layout_key VARCHAR(191) NOT NULL,
    draft_experience_json LONGTEXT NULL,
    published_experience_json LONGTEXT NULL,
    published_by VARCHAR(191) NULL,
    published_at DATETIME NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @baseline_experience = '{"slug":"kit-whatsapp-pronto","name":"Kit WhatsApp Pronto","experienceVersion":"kit-whatsapp-pde-v1","layoutKey":"assisted-service-v1","funnelVersion":"pde-assisted-service-v1","promise":"Contrato comercial anterior"}';

INSERT INTO product (id, slug, pde_experience_json, primary_cta, updated_at)
VALUES (9, 'kit-whatsapp-pronto', @baseline_experience, 'Quero meu atendimento personalizado', CURRENT_TIMESTAMP);

INSERT INTO experiment (id, funnel_promise, primary_cta, updated_at)
VALUES (89, 'Contrato comercial anterior', 'Quero meu atendimento personalizado', CURRENT_TIMESTAMP);

INSERT INTO pde_production_slot (
    id,
    product_slug,
    source_experiment_id,
    experience_version,
    layout_key,
    draft_experience_json,
    published_experience_json,
    published_by,
    published_at,
    updated_at
)
VALUES (
    1,
    'kit-whatsapp-pronto',
    89,
    'kit-whatsapp-pde-v1',
    'assisted-service-v1',
    @baseline_experience,
    @baseline_experience,
    'fixture:rigel-v1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
