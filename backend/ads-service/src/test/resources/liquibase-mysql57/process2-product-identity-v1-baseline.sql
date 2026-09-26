SET NAMES utf8mb4;

CREATE TABLE business_process_definition (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    process_code VARCHAR(191) NOT NULL,
    name VARCHAR(191) NOT NULL,
    purpose TEXT NULL,
    owner_name VARCHAR(191) NULL,
    trigger_description TEXT NULL,
    outcome_description TEXT NULL,
    version_number INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    technical_reference VARCHAR(512) NULL,
    process_type VARCHAR(64) NULL,
    parent_process_code VARCHAR(191) NULL,
    execution_scope VARCHAR(64) NULL,
    diagram_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    UNIQUE KEY uk_process_version (process_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_process_activity_definition (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    process_definition_id BIGINT NOT NULL,
    activity_id VARCHAR(191) NOT NULL,
    name VARCHAR(191) NOT NULL,
    objective TEXT NULL,
    owner_name VARCHAR(191) NULL,
    execution_resource_code VARCHAR(191) NULL,
    subprocess_code VARCHAR(191) NULL,
    definition_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_process_activity (process_definition_id, activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_process_chain_definition (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chain_code VARCHAR(191) NOT NULL,
    name VARCHAR(191) NOT NULL,
    purpose TEXT NULL,
    outcome_description TEXT NULL,
    primary_metric VARCHAR(191) NULL,
    version_number INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    UNIQUE KEY uk_chain_version (chain_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_process_chain_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chain_definition_id BIGINT NOT NULL,
    process_definition_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    value_contribution TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_chain_sequence (chain_definition_id, sequence_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_process_independent_execution (
    id BIGINT NOT NULL PRIMARY KEY,
    source_reference VARCHAR(191) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_type_definition (
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(191) NOT NULL,
    internal_name VARCHAR(191) NOT NULL,
    status VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_product_type_code (code),
    UNIQUE KEY uk_product_type_internal_name (internal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(191) NULL,
    slug VARCHAR(191) NULL,
    internal_name VARCHAR(191) NULL,
    product_type_id BIGINT NULL,
    product_type VARCHAR(191) NULL,
    validation_definition_json LONGTEXT NULL,
    pde_experience_json LONGTEXT NULL,
    commercial_notes LONGTEXT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_alias (
    product_id BIGINT NOT NULL,
    alias VARCHAR(191) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE opportunity_dossier (
    id BIGINT NOT NULL PRIMARY KEY,
    product_discovery_cycle_id BIGINT NOT NULL,
    created_product_id BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO business_process_definition (
    id, process_code, name, purpose, owner_name, trigger_description, outcome_description,
    version_number, status, technical_reference, process_type, parent_process_code,
    execution_scope, diagram_json, created_at, published_at
) VALUES (
    94,
    'pde-commercial-plan-offer',
    'Estratégia, economia e protótipo privado do PDE',
    'Definir a oferta privada.',
    'Atena',
    'Dossiê factual pronto.',
    'Produto planejado.',
    8,
    'PUBLISHED',
    'fixture:v8',
    'BUSINESS',
    NULL,
    'INDEPENDENT',
    '{"schemaVersion":"PDE_COMMERCIAL_PLAN_OFFER_V8","nodes":[{"id":"start","label":"Início","description":"Iniciar.","owner":"Backend","executionResourceCode":"backend","subprocessCode":null,"type":"START"},{"id":"marketStrategy","label":"Estratégia de mercado","description":"Selecionar a oferta.","owner":"Atena","executionResourceCode":"experiment-strategist","subprocessCode":null,"type":"TASK"},{"id":"economics","label":"Economia","description":"Validar a economia.","owner":"Plutus","executionResourceCode":"financial-agent","subprocessCode":null,"type":"TASK"},{"id":"productArchitecture","label":"Arquitetura","description":"Projetar o protótipo.","owner":"Dédalo","executionResourceCode":"landing-generator","subprocessCode":null,"type":"TASK"}]}',
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
);

INSERT INTO business_process_activity_definition (
    process_definition_id, activity_id, name, objective, owner_name,
    execution_resource_code, subprocess_code, definition_json, created_at
) VALUES
    (94, 'marketStrategy', 'Estratégia de mercado', 'Selecionar a oferta.', 'Atena', 'experiment-strategist', NULL, '{}', UTC_TIMESTAMP(6)),
    (94, 'economics', 'Economia', 'Validar a economia.', 'Plutus', 'financial-agent', NULL, '{}', UTC_TIMESTAMP(6)),
    (94, 'productArchitecture', 'Arquitetura', 'Projetar o protótipo.', 'Dédalo', 'landing-generator', NULL, '{}', UTC_TIMESTAMP(6));

INSERT INTO business_process_chain_definition (
    id, chain_code, name, purpose, outcome_description, primary_metric,
    version_number, status, created_at, published_at
) VALUES (
    22,
    'pde-value-creation-delivery',
    'Cadeia de valor PDE',
    'Transformar oportunidade em produto.',
    'Produto entregue com valor.',
    'Compras líquidas com margem',
    22,
    'PUBLISHED',
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
);

INSERT INTO business_process_chain_item (
    chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at
) VALUES (22, 94, 2, 'Define estratégia e economia antes da construção.', UTC_TIMESTAMP(6));

INSERT INTO product_type_definition (id, code, name, internal_name, status) VALUES
    (1, 'PDE', 'Produto Digital Especializado', 'Opala', 'ACTIVE'),
    (2, 'AI_PRODUCT', 'Produto de Inteligência Artificial', 'Safira', 'ACTIVE');

INSERT INTO product (
    id, name, slug, internal_name, product_type_id, product_type,
    validation_definition_json, pde_experience_json, commercial_notes, updated_at
) VALUES
    (10, 'Mira', 'mira', 'Mira', 2, 'Produto de Inteligência Artificial', '{}', '{}', 'Produto existente.', UTC_TIMESTAMP(6)),
    (11, 'Decisão de look para uma ocasião específica · PDE planejado #46', 'pde-planejado-46',
     'Decisão de look para uma ocasião específica · PDE planejado #46', 1,
     'Produto Digital Especializado', '{}', '{}', 'Materialização provisória.', UTC_TIMESTAMP(6));

INSERT INTO product_alias (product_id, alias) VALUES (10, 'Mira IA');
INSERT INTO business_process_independent_execution (id, source_reference)
VALUES (32, 'product-discovery-cycle:71');
INSERT INTO opportunity_dossier (id, product_discovery_cycle_id, created_product_id)
VALUES (46, 71, 11);
