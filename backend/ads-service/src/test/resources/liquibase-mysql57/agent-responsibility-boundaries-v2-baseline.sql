CREATE TABLE agent_theme (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  description TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_agent_theme_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO agent_theme (id, name, description)
VALUES (1, 'Operacoes Autonomas', 'Agentes governados por metas, evidencias e limites de autoridade.');

CREATE TABLE agent (
  id BIGINT NOT NULL AUTO_INCREMENT,
  theme_id BIGINT NULL,
  agent_key VARCHAR(100) NOT NULL,
  name VARCHAR(160) NOT NULL,
  nickname VARCHAR(60) NULL,
  current_version INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  execution_mode VARCHAR(50) NULL,
  automatic_execution_enabled TINYINT(1) NOT NULL DEFAULT 1,
  model_name VARCHAR(100) NULL,
  description TEXT NULL,
  owner_name VARCHAR(255) NULL,
  business_objective TEXT NULL,
  success_metrics TEXT NULL,
  trigger_policy TEXT NULL,
  authority_policy LONGTEXT NULL,
  responsibility_contract TEXT NULL,
  orchestrator_policy TEXT NULL,
  analysis_policy TEXT NULL,
  offering_policy TEXT NULL,
  prompt_contract_path VARCHAR(500) NULL,
  schema_contract_path VARCHAR(500) NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_agent_key (agent_key),
  UNIQUE KEY uk_fixture_agent_nickname (nickname),
  CONSTRAINT fk_fixture_agent_theme FOREIGN KEY (theme_id) REFERENCES agent_theme(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_input (
  id BIGINT NOT NULL AUTO_INCREMENT,
  agent_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  input_type VARCHAR(100) NULL,
  description LONGTEXT NULL,
  order_index INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_fixture_agent_input_agent FOREIGN KEY (agent_id) REFERENCES agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_output (
  id BIGINT NOT NULL AUTO_INCREMENT,
  agent_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  output_type VARCHAR(100) NULL,
  description LONGTEXT NULL,
  order_index INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_fixture_agent_output_agent FOREIGN KEY (agent_id) REFERENCES agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_internal_function (
  id BIGINT NOT NULL AUTO_INCREMENT,
  agent_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  function_type VARCHAR(100) NULL,
  description LONGTEXT NULL,
  order_index INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_fixture_agent_function_agent FOREIGN KEY (agent_id) REFERENCES agent(id)
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

CREATE TABLE business_process_execution_resource (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resource_code VARCHAR(100) NOT NULL,
  name VARCHAR(160) NOT NULL,
  description VARCHAR(500) NOT NULL,
  resource_type VARCHAR(30) NOT NULL,
  responsible_agent_key VARCHAR(100) NOT NULL,
  executor_reference VARCHAR(160) NOT NULL,
  usage_instructions TEXT NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_execution_resource (resource_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO business_process_execution_resource
  (resource_code, name, description, resource_type, responsible_agent_key,
   executor_reference, usage_instructions, active, created_at, updated_at)
VALUES
  ('themis-image-studio', 'Estúdio de Imagens de Têmis',
   'Recurso histórico de produção visual.', 'CONTAINER', 'meta-ad-approver',
   'themis-image-studio', 'Execução histórica.', 1, UTC_TIMESTAMP(), UTC_TIMESTAMP());

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
   'prompts/meta-ad-approver/v1/approver-schema.json'),
  (4, 'market-radar', 'Agente Radar de Mercado', 2, 'READY', 'gpt-5.6-sol',
   'product-discovery-worker/prompts/productdiscovery.v1/research/system.md',
   'product-discovery-worker/prompts/productdiscovery.v1/research/response-schema.json'),
  (5, 'financial-agent', 'Agente Financeiro', 3, 'READY', 'gpt-5.6-sol',
   'financial-agent-worker/src/main/resources/prompts/financial-agent/v1/report.md',
   'financial-agent-worker/src/main/resources/prompts/financial-agent/v1/report-schema.json'),
  (6, 'landing-generator', 'Agente Gerador de Landing', 2, 'READY', 'gpt-5.6-sol',
   'landing-generator-agent-worker/src/main/resources/prompts/landing-generator/v1/html.md',
   'landing-generator-agent-worker/src/main/resources/prompts/landing-generator/v1/html-schema.json'),
  (7, 'videomaker', 'Agente Videomaker', 2, 'READY', 'gpt-5.6-sol',
   'video-management-service/src/main/resources/prompts/apollo/v2/production-mission.md',
   'video-management-service/src/main/resources/prompts/apollo/v2/production-mission-schema.json'),
  (8, 'customer-agent', 'Agente Cliente', 3, 'READY', 'gpt-5.6-sol',
   'customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v3.md',
   'customer-agent-worker/src/main/resources/prompts/customer-agent/behavioral-v3/evaluation-schema.json');

INSERT INTO agent_version (agent_id, version_number, contract_snapshot, created_at)
VALUES
  (1, 3, '{"historical":true}', UTC_TIMESTAMP()),
  (2, 4, '{"historical":true}', UTC_TIMESTAMP()),
  (3, 2, '{"historical":true}', UTC_TIMESTAMP()),
  (4, 2, '{"historical":true}', UTC_TIMESTAMP()),
  (5, 3, '{"historical":true}', UTC_TIMESTAMP()),
  (6, 2, '{"historical":true}', UTC_TIMESTAMP()),
  (7, 2, '{"historical":true}', UTC_TIMESTAMP()),
  (8, 3, '{"historical":true}', UTC_TIMESTAMP());

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
   4, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (7, 'creative-production-approval', 'Criativos', 'Criar', 'Marketing Hub', 'Plano', 'Criativos',
   6, 'PUBLISHED', NULL, 'SUBPROCESS', 'pde-communication-sales-journey', '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (8, 'experiment-homologation-activation', 'Preflight', 'Homologar', 'Marketing Hub', 'Jornada', 'Preflight',
   4, 'PUBLISHED', NULL, 'SUBPROCESS', 'pde-commercial-homologation-activation', '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (9, 'landing-page-generation', 'Landing', 'Construir', 'Marketing Hub', 'Plano', 'Landing',
   4, 'PUBLISHED', NULL, 'SUBPROCESS', 'pde-communication-sales-journey', '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (10, 'operacao-otimizacao-experimento', 'Otimização', 'Otimizar', 'Marketing Hub', 'RUNNING', 'Decisão',
   3, 'PUBLISHED', NULL, 'SUBPROCESS', 'pde-sales-delivery-learning', '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (11, 'pde-tasting-proof-of-value', 'Degustação', 'Provar valor', 'Marketing Hub', 'Produto', 'Decisão',
   1, 'PUBLISHED', NULL, 'SUBPROCESS', NULL, '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  (12, 'venda-entrega-satisfacao-cliente', 'Entrega', 'Entregar venda', 'Marketing Hub', 'Pagamento', 'Satisfação',
   3, 'PUBLISHED', NULL, 'SUBPROCESS', 'pde-sales-delivery-learning', '{}', UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT INTO business_process_chain_definition
  (id, chain_code, name, purpose, outcome_description, primary_metric,
   version_number, status, created_at, published_at)
VALUES
  (1, 'pde-value-creation-delivery', 'Cadeia histórica', 'Histórico', 'Histórico',
   'Tempo até venda', 6, 'PUBLISHED', UTC_TIMESTAMP(), UTC_TIMESTAMP());

CREATE TABLE opportunity_dossier (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(191) NOT NULL,
  owner_agent_key VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  target_audience VARCHAR(512) NOT NULL,
  main_pain VARCHAR(512) NOT NULL,
  reference_product VARCHAR(512) NOT NULL,
  ai_advantage LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE opportunity_agent_review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dossier_id BIGINT NOT NULL,
  agent_key VARCHAR(64) NOT NULL,
  decision VARCHAR(24) NULL,
  rationale LONGTEXT NULL,
  requested_at DATETIME NOT NULL,
  completed_at DATETIME NULL,
  execution_status VARCHAR(24) NOT NULL,
  started_at DATETIME NULL,
  updated_at DATETIME NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  error_message LONGTEXT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fixture_opportunity_review_agent (dossier_id, agent_key),
  CONSTRAINT fk_fixture_opportunity_review_dossier
    FOREIGN KEY (dossier_id) REFERENCES opportunity_dossier(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO opportunity_dossier
  (id, title, owner_agent_key, status, target_audience, main_pain, reference_product,
   ai_advantage, created_at, updated_at)
VALUES
  (1, 'Oportunidade histórica', 'ARGOS', 'UNDER_REVIEW', 'Profissionais', 'Esforço',
   'Produto de referência', 'Experiência simples com IA', UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT INTO opportunity_agent_review
  (id, dossier_id, agent_key, decision, rationale, requested_at, completed_at,
   execution_status, started_at, updated_at, retry_count, error_message)
VALUES
  (1, 1, 'ATENA', 'SUPPORT', 'Estratégia sustentada.', UTC_TIMESTAMP(), UTC_TIMESTAMP(),
   'COMPLETED', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0, NULL),
  (2, 1, 'PSIQUE', NULL, NULL, UTC_TIMESTAMP(), NULL,
   'PENDING', NULL, UTC_TIMESTAMP(), 0, NULL),
  (3, 1, 'PLUTUS', NULL, NULL, UTC_TIMESTAMP(), NULL,
   'RUNNING', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0, NULL),
  (4, 1, 'HERMES', 'ADJUST', 'Parecer histórico concluído.', UTC_TIMESTAMP(), UTC_TIMESTAMP(),
   'COMPLETED', UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0, NULL);
