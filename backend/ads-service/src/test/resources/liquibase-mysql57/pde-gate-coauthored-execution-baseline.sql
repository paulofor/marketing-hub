CREATE TABLE business_process_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  process_code VARCHAR(100) NOT NULL,
  name VARCHAR(160) NOT NULL,
  purpose TEXT NOT NULL,
  owner_name VARCHAR(120) NOT NULL,
  trigger_description VARCHAR(500) NOT NULL,
  outcome_description VARCHAR(500) NOT NULL,
  version_number INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  technical_reference VARCHAR(200) NULL,
  process_type VARCHAR(20) NOT NULL DEFAULT 'VALUE_PROCESS',
  parent_process_code VARCHAR(100) NULL,
  diagram_json LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_process_code_version (process_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_activity_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  process_definition_id BIGINT NOT NULL,
  activity_id VARCHAR(100) NOT NULL,
  name VARCHAR(160) NOT NULL,
  objective TEXT NULL,
  owner_name VARCHAR(160) NULL,
  execution_resource_code VARCHAR(100) NULL,
  subprocess_code VARCHAR(100) NULL,
  definition_json LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_process_activity_definition (process_definition_id, activity_id),
  CONSTRAINT fk_pde_gate_fixture_activity_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_chain_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chain_code VARCHAR(100) NOT NULL,
  name VARCHAR(160) NOT NULL,
  purpose TEXT NOT NULL,
  outcome_description VARCHAR(500) NOT NULL,
  primary_metric VARCHAR(200) NOT NULL,
  version_number INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_process_chain_code_version (chain_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_chain_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chain_definition_id BIGINT NOT NULL,
  process_definition_id BIGINT NOT NULL,
  sequence_number INT NOT NULL,
  value_contribution VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_process_chain_sequence (chain_definition_id, sequence_number),
  UNIQUE KEY uk_business_process_chain_process (chain_definition_id, process_definition_id),
  CONSTRAINT fk_pde_gate_fixture_item_chain
    FOREIGN KEY (chain_definition_id) REFERENCES business_process_chain_definition(id),
  CONSTRAINT fk_pde_gate_fixture_item_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO business_process_definition
  (id, process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   diagram_json, created_at, published_at)
VALUES
  (37, 'pde-opportunity-discovery', 'Descoberta', 'Escolher oportunidade.', 'Mercado', 'Sinais.', 'Oportunidade.', 4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{"nodes":[]}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (38, 'pde-commercial-plan-offer', 'Plano comercial', 'Planejar oferta.', 'Planejamento', 'Oportunidade.', 'Plano.', 4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{"nodes":[]}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (39, 'pde-construction-approval', 'Construção', 'Construir produto.', 'Produto', 'Plano.', 'Produto.', 4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{"nodes":[]}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (43, 'pde-communication-sales-journey', 'Comunicação', 'Criar jornada.', 'Experimentos', 'Produto.', 'Jornada.', 4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{"nodes":[]}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (45, 'pde-commercial-homologation-activation', 'Homologação', 'Homologar PDE.', 'Experimentos', 'Jornada.', 'Decisão.', 4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{"nodes":[]}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (46, 'pde-sales-delivery-learning', 'Venda e entrega', 'Vender e entregar.', 'Crescimento', 'Ativação.', 'Aprendizado.', 4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{"nodes":[]}', UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT INTO business_process_chain_definition
  (id, chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
VALUES
  (5, 'pde-value-creation-delivery', 'Cadeia PDE', 'Transformar valor.', 'Venda entregue.',
   'Tempo até venda entregue', 5, 'PUBLISHED', UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT INTO business_process_chain_item
  (chain_definition_id, process_definition_id, sequence_number, value_contribution, created_at)
SELECT 5, id,
  CASE process_code
    WHEN 'pde-opportunity-discovery' THEN 1
    WHEN 'pde-commercial-plan-offer' THEN 2
    WHEN 'pde-construction-approval' THEN 3
    WHEN 'pde-communication-sales-journey' THEN 4
    WHEN 'pde-commercial-homologation-activation' THEN 5
    ELSE 6
  END,
  'Contribuição vigente.', UTC_TIMESTAMP()
FROM business_process_definition;
