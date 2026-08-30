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
  CONSTRAINT fk_audit_activity_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent (
  id BIGINT NOT NULL AUTO_INCREMENT,
  agent_key VARCHAR(100) NOT NULL,
  current_version INT NOT NULL DEFAULT 3,
  PRIMARY KEY (id),
  UNIQUE KEY uk_audit_agent_key (agent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE business_process_activity_instance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  activity_definition_id BIGINT NOT NULL,
  source_reference VARCHAR(200) NOT NULL,
  occurrence_number INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  entered_at DATETIME NOT NULL,
  exited_at DATETIME NULL,
  objective_achieved TINYINT(1) NOT NULL DEFAULT 0,
  objective_evidence_json LONGTEXT NULL,
  blocked_reason LONGTEXT NULL,
  known_cost_usd DECIMAL(18,8) NULL,
  cost_coverage VARCHAR(32) NOT NULL DEFAULT 'NOT_REPORTED',
  evidence_quality VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_process_activity_instance
    (activity_definition_id, source_reference, occurrence_number),
  CONSTRAINT fk_audit_instance_definition
    FOREIGN KEY (activity_definition_id) REFERENCES business_process_activity_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  assigned_agent_id BIGINT NOT NULL,
  requested_by_agent_id BIGINT NULL,
  requested_by_type VARCHAR(20) NOT NULL,
  requested_by_name VARCHAR(100) NOT NULL,
  title VARCHAR(160) NOT NULL,
  description LONGTEXT NOT NULL,
  priority VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL,
  source_reference VARCHAR(200) NULL,
  process_definition_id BIGINT NULL,
  process_activity_id VARCHAR(100) NULL,
  process_activity_name VARCHAR(160) NULL,
  activity_instance_id BIGINT NULL,
  exceptional TINYINT(1) NOT NULL DEFAULT 0,
  exception_reason VARCHAR(500) NULL,
  task_kind VARCHAR(30) NOT NULL DEFAULT 'WORK',
  gate_code VARCHAR(100) NULL,
  gate_status VARCHAR(30) NULL,
  gate_decision_reason LONGTEXT NULL,
  gate_decided_at DATETIME NULL,
  received_at DATETIME NULL,
  delivered_at DATETIME NULL,
  result_json LONGTEXT NULL,
  evidence_json LONGTEXT NULL,
  execution_error LONGTEXT NULL,
  input_tokens BIGINT NULL,
  cached_input_tokens BIGINT NULL,
  output_tokens BIGINT NULL,
  estimated_cost_usd DECIMAL(18,8) NULL,
  cost_estimation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REPORTED',
  model_usage_updated_at DATETIME NULL,
  execution_model_code VARCHAR(128) NULL,
  execution_reasoning_effort VARCHAR(32) NULL,
  execution_prompt LONGTEXT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_task_agent FOREIGN KEY (assigned_agent_id) REFERENCES agent(id),
  CONSTRAINT fk_audit_task_requester FOREIGN KEY (requested_by_agent_id) REFERENCES agent(id),
  CONSTRAINT fk_audit_task_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id),
  CONSTRAINT fk_audit_task_instance
    FOREIGN KEY (activity_instance_id) REFERENCES business_process_activity_instance(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_discovery_cycle (
  id BIGINT NOT NULL AUTO_INCREMENT,
  theme VARCHAR(191) NOT NULL,
  target_audience VARCHAR(191) NULL,
  country VARCHAR(16) NOT NULL DEFAULT 'BR',
  language VARCHAR(16) NOT NULL DEFAULT 'pt-BR',
  acquisition_channel VARCHAR(120) NULL,
  commercial_constraints LONGTEXT NULL,
  forbidden_categories LONGTEXT NULL,
  objective LONGTEXT NULL,
  status ENUM('DRAFT','READY_FOR_RESEARCH','RESEARCHING','COMPLETED','FAILED','ARCHIVED') NOT NULL,
  stage_code VARCHAR(80) NOT NULL DEFAULT 'research',
  decision_summary LONGTEXT NULL,
  error_message LONGTEXT NULL,
  execution_lease_id VARCHAR(36) NULL,
  lease_expires_at DATETIME NULL,
  execution_attempt INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  research_plan_json LONGTEXT NULL,
  research_plan_raw_response LONGTEXT NULL,
  research_plan_model VARCHAR(120) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_discovery_opportunity (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cycle_id BIGINT NOT NULL,
  name VARCHAR(191) NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  decision VARCHAR(40) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_opportunity_cycle
    FOREIGN KEY (cycle_id) REFERENCES product_discovery_cycle(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO business_process_definition
  (id, process_code, name, purpose, owner_name, trigger_description, outcome_description,
   version_number, status, technical_reference, process_type, parent_process_code,
   diagram_json, created_at, published_at)
VALUES
  (37, 'pde-opportunity-discovery', 'Descoberta v4', 'Escolher oportunidade.',
   'Argos', 'Sinal humano.', 'Oportunidade priorizada.', 4, 'RETIRED', NULL,
   'VALUE_PROCESS', NULL, '{"nodes":[]}', '2026-08-22 01:46:54', '2026-08-22 01:46:54'),
  (49, 'pde-opportunity-discovery', 'Descoberta e priorização da oportunidade PDE',
   'Escolher oportunidade real.', 'Argos e Dédalo', 'Sinal humano.',
   'Oportunidade priorizada.', 5, 'PUBLISHED', NULL, 'VALUE_PROCESS', NULL,
   '{"nodes":[{"id":"inspiration","type":"TASK","label":"Qualificar fontes e inspirações atualizadas","owner":"Argos e Dédalo"},{"id":"evidence","type":"TASK","label":"Confirmar evidências","owner":"Argos"}],"flows":[]}',
   '2026-08-26 06:36:14', '2026-08-26 21:36:50');

INSERT INTO business_process_activity_definition
  (id, process_definition_id, activity_id, name, objective, owner_name,
   execution_resource_code, subprocess_code, definition_json, created_at)
VALUES
  (555, 49, 'inspiration', 'Qualificar fontes e inspirações atualizadas',
   'Confirmar fontes atuais.', 'Argos e Dédalo', NULL, NULL,
   '{"id":"inspiration","type":"TASK","label":"Qualificar fontes e inspirações atualizadas","owner":"Argos e Dédalo"}',
   '2026-08-26 21:36:50'),
  (556, 49, 'evidence', 'Confirmar evidências', 'Confirmar demanda.',
   'Argos', NULL, NULL,
   '{"id":"evidence","type":"TASK","label":"Confirmar evidências","owner":"Argos"}',
   '2026-08-26 21:36:50');

INSERT INTO agent (id, agent_key) VALUES (8, 'market-radar');

INSERT INTO product_discovery_cycle
  (id, theme, target_audience, country, language, objective, status, stage_code,
   decision_summary, error_message, execution_attempt, created_at, updated_at,
   research_plan_json, research_plan_raw_response, research_plan_model)
VALUES
  (37, 'Auditoria de saída de imóvel', 'Locatários', 'BR', 'pt-BR',
   'Encontrar oportunidade comparável ao Rigel.', 'FAILED', 'research', NULL,
   'POST complete falhou com status 422', 1, '2026-08-27 23:09:01', '2026-08-27 23:11:01',
   '{"questions":["Qual dor é urgente?"]}', '{"questions":["Qual dor é urgente?"]}',
   'deterministic-fallback-v1'),
  (38, 'Sinistro automotivo travado', 'Condutores', 'BR', 'pt-BR',
   'Encontrar oportunidade comparável ao Rigel.', 'FAILED', 'research', NULL,
   'POST complete falhou com status 422', 1, '2026-08-27 23:12:01', '2026-08-27 23:14:01',
   '{"questions":["Qual custo do atraso?"]}', '{"questions":["Qual custo do atraso?"]}',
   'deterministic-fallback-v1'),
  (39, 'Voo cancelado e reembolso', 'Passageiros', 'BR', 'pt-BR',
   'Encontrar oportunidade comparável ao Rigel.', 'FAILED', 'research', NULL,
   'POST complete falhou com status 422', 1, '2026-08-27 23:15:01', '2026-08-27 23:17:01',
   '{"questions":["Qual prazo material?"]}', '{"questions":["Qual prazo material?"]}',
   'deterministic-fallback-v1'),
  (40, 'Proposta pronta', 'Prestadores', 'BR', 'pt-BR', 'Validar uma oportunidade.',
   'COMPLETED', 'opportunity-gate', 'Pesquisar mais antes de investir.', NULL, 1,
   '2026-08-27 23:18:01', '2026-08-27 23:20:01',
   '{"questions":["Qual valor?"]}', '{"questions":["Qual valor?"]}', 'gpt-5.6-sol'),
  (41, 'Agenda guiada', 'Autônomos', 'BR', 'pt-BR', 'Validar uma oportunidade.',
   'READY_FOR_RESEARCH', 'research', NULL, NULL, 0,
   '2026-08-27 23:21:01', '2026-08-27 23:21:01', NULL, NULL, NULL),
  (42, 'Diagnóstico simples', 'Consumidores', 'BR', 'pt-BR', 'Validar uma oportunidade.',
   'RESEARCHING', 'research', NULL, NULL, 1,
   '2026-08-27 23:22:01', '2026-08-27 23:23:01',
   '{"questions":["Qual decisão?"]}', '{"questions":["Qual decisão?"]}', 'gpt-5.6-sol');

INSERT INTO product_discovery_opportunity (cycle_id, name, score, decision)
VALUES (40, 'Proposta pronta em dez minutos', 76.00, 'RESEARCH_MORE');
