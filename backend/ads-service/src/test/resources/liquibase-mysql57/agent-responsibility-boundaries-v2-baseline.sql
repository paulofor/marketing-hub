CREATE TABLE agent (
  id BIGINT NOT NULL AUTO_INCREMENT,
  agent_key VARCHAR(100) NOT NULL,
  name VARCHAR(160) NOT NULL,
  current_version INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  model_name VARCHAR(100) NULL,
  description TEXT NULL,
  business_objective TEXT NULL,
  success_metrics TEXT NULL,
  responsibility_contract TEXT NULL,
  orchestrator_policy TEXT NULL,
  analysis_policy TEXT NULL,
  offering_policy TEXT NULL,
  prompt_contract_path VARCHAR(500) NULL,
  schema_contract_path VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_agent_key (agent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  agent_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  contract_snapshot TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_agent_version (agent_id, version_number),
  CONSTRAINT fk_fixture_agent_version_agent FOREIGN KEY (agent_id) REFERENCES agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  process_code VARCHAR(120) NOT NULL,
  name VARCHAR(200) NOT NULL,
  purpose TEXT NOT NULL,
  owner_name VARCHAR(160) NOT NULL,
  trigger_description TEXT NOT NULL,
  outcome_description TEXT NOT NULL,
  version_number INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  technical_reference VARCHAR(255) NULL,
  process_type VARCHAR(40) NOT NULL,
  parent_process_code VARCHAR(120) NULL,
  diagram_json LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_process_version (process_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_activity_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  process_definition_id BIGINT NOT NULL,
  activity_id VARCHAR(100) NOT NULL,
  name VARCHAR(200) NOT NULL,
  objective TEXT NOT NULL,
  owner_name VARCHAR(160) NOT NULL,
  execution_resource_code VARCHAR(100) NULL,
  subprocess_code VARCHAR(120) NULL,
  definition_json LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_activity (process_definition_id, activity_id),
  CONSTRAINT fk_fixture_activity_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_chain_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chain_code VARCHAR(120) NOT NULL,
  name VARCHAR(200) NOT NULL,
  purpose TEXT NOT NULL,
  outcome_description TEXT NOT NULL,
  primary_metric VARCHAR(255) NOT NULL,
  version_number INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_chain_version (chain_code, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_chain_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chain_definition_id BIGINT NOT NULL,
  process_definition_id BIGINT NOT NULL,
  sequence_number INT NOT NULL,
  value_contribution TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_chain_sequence (chain_definition_id, sequence_number),
  CONSTRAINT fk_fixture_chain_item_chain
    FOREIGN KEY (chain_definition_id) REFERENCES business_process_chain_definition(id),
  CONSTRAINT fk_fixture_chain_item_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO agent
  (id, agent_key, name, current_version, status, model_name, prompt_contract_path, schema_contract_path)
VALUES
  (1, 'experiment-strategist', 'Estrategista de Experimentos', 3, 'READY', 'gpt-5.6-sol',
   'experiment-strategist-worker/src/main/resources/prompts/experiment-strategist/v1/research.md',
   'experiment-strategist-worker/src/main/resources/prompts/experiment-strategist/v1/research-schema.json'),
  (2, 'growth-operator', 'Operador de Crescimento', 4, 'READY', 'gpt-5.6-sol',
   'growth-operator-worker/src/main/resources/prompts/growth-operator/v1/diagnosis.md',
   'growth-operator-worker/src/main/resources/prompts/growth-operator/v1/diagnosis-schema.json'),
  (3, 'meta-ad-approver', 'Agente Criador e Aprovador de Anuncios Meta', 2, 'READY', 'gpt-5.6-sol',
   'prompts/meta-ad-approver/v1/approver.md',
   'prompts/meta-ad-approver/v1/approver-schema.json');

INSERT INTO agent_version (agent_id, version_number, contract_snapshot, created_at)
VALUES
  (1, 3, '{"historical":true}', UTC_TIMESTAMP()),
  (2, 4, '{"historical":true}', UTC_TIMESTAMP()),
  (3, 2, '{"historical":true}', UTC_TIMESTAMP());

INSERT INTO business_process_definition
  (id, process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   diagram_json, created_at, published_at)
VALUES
  (1, 'pde-opportunity-discovery', 'Descoberta', 'Descobrir', 'Marketing Hub', 'Início', 'Oportunidade',
   5, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (2, 'pde-commercial-plan-offer', 'Plano', 'Planejar', 'Marketing Hub', 'Oportunidade', 'Plano',
   4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (3, 'pde-construction-approval', 'Construção', 'Construir', 'Marketing Hub', 'Plano', 'Produto',
   4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (4, 'pde-communication-sales-journey', 'Comunicação', 'Comunicar', 'Marketing Hub', 'Produto', 'Jornada',
   4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (5, 'pde-commercial-homologation-activation', 'Homologação', 'Homologar', 'Marketing Hub', 'Jornada', 'Decisão',
   5, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (6, 'pde-sales-delivery-learning', 'Entrega', 'Entregar', 'Marketing Hub', 'Venda', 'Aprendizado',
   4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT INTO business_process_chain_definition
  (id, chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
VALUES
  (1, 'pde-value-creation-delivery', 'Cadeia histórica', 'Histórico', 'Histórico',
   'Tempo até venda', 6, 'PUBLISHED', UTC_TIMESTAMP(), UTC_TIMESTAMP());
