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
    validation_definition_version VARCHAR(191) NULL,
    validation_definition_json LONGTEXT NULL,
    pde_experience_json LONGTEXT NULL,
    commercial_notes LONGTEXT NULL,
    commercial_status VARCHAR(32) NULL,
    automatic_execution_enabled TINYINT(1) NOT NULL DEFAULT 0,
    automatic_execution_changed_at DATETIME(6) NULL,
    automatic_execution_changed_by VARCHAR(191) NULL,
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

CREATE TABLE opportunity_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dossier_id BIGINT NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    summary LONGTEXT NOT NULL,
    created_by VARCHAR(191) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_process_activity_instance (
    id BIGINT NOT NULL PRIMARY KEY,
    source_reference VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_task (
    id BIGINT NOT NULL PRIMARY KEY,
    status VARCHAR(30) NOT NULL,
    source_reference VARCHAR(200) NULL,
    title VARCHAR(160) NOT NULL,
    description LONGTEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_process_run_v1 (
    id BIGINT NOT NULL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    process_definition_id BIGINT NOT NULL,
    chain_definition_id BIGINT NOT NULL,
    learning_cycle_id BIGINT NULL,
    scope_key VARCHAR(64) NOT NULL,
    source_reference VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_reconciled_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    revision BIGINT NOT NULL,
    UNIQUE KEY uk_product_process_run_scope (scope_key)
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
    validation_definition_version, validation_definition_json, pde_experience_json,
    commercial_notes, commercial_status, automatic_execution_enabled,
    automatic_execution_changed_at, automatic_execution_changed_by, updated_at
) VALUES
    (10, 'Mira', 'mira', 'Mira', 2, 'Produto de Inteligência Artificial',
     'PDE_AGENT_VALIDATED_V1', '{}', '{}', 'Produto existente.', 'ATIVO', 0,
     UTC_TIMESTAMP(6), 'fixture', UTC_TIMESTAMP(6)),
    (11, 'Decisão de look para uma ocasião específica · PDE planejado #46', 'pde-planejado-46',
     'Decisão de look para uma ocasião específica · PDE planejado #46', 1,
     'Produto Digital Especializado', 'PDE_PRIVATE_VALIDATION_V1',
     '{"privateValidationPlan":{"minimumIndependentReadings":2},"productIdentity":{"contractVersion":"LEGACY"}}',
     '{"contractVersion":"PDE_HARNESS_PLAN_V1","experienceVersion":"private-validation-v1","lineage":{"cycleId":71,"dossierId":46,"opportunityId":66,"commercialPlanId":34},"economics":{"offerPriceBrl":79,"variableCostPerSaleBrl":24,"privateReadingsTarget":2,"commercialSpendAuthorized":false},"privateValidationPlan":{"minimumIndependentReadings":2},"productIdentity":{"contractVersion":"LEGACY"}}',
     'Materialização provisória.', 'PLANNED', 1, UTC_TIMESTAMP(6), 'fixture', UTC_TIMESTAMP(6));

INSERT INTO product_alias (product_id, alias) VALUES (10, 'Mira IA');
INSERT INTO business_process_independent_execution (id, source_reference)
VALUES (32, 'product-discovery-cycle:71');
INSERT INTO opportunity_dossier (id, product_discovery_cycle_id, created_product_id)
VALUES (46, 71, 11);

INSERT INTO business_process_activity_instance (id, source_reference, status, updated_at)
VALUES (414, 'product:11@private-validation-v1', 'IN_PROGRESS', UTC_TIMESTAMP(6));

INSERT INTO agent_task (id, status, source_reference, title, description, updated_at)
VALUES (
    534,
    'IN_PROGRESS',
    'product:11@private-validation-v1',
    'Construir jornada privada de valor · Alcyone',
    'Entregar jornada privada ainda vinculada ao contrato humano legado.',
    UTC_TIMESTAMP(6)
);

INSERT INTO product_process_run_v1 (
    id, product_id, process_definition_id, chain_definition_id, learning_cycle_id,
    scope_key, source_reference, status, reason, updated_at, last_reconciled_at,
    finished_at, revision
) VALUES (
    26, 11, 84, 22, NULL,
    '92d6b26c657d64c1a507a020dcaf2bdbc6497b189f3aa1a3ae58cdf8cac4a1c5',
    'product:11@private-validation-v1', 'WAITING_ACTIVITY',
    'Atividade aguardando execução ou liberação pelo backend.',
    UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL, 1
);
