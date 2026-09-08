CREATE TABLE IF NOT EXISTS agent_task (id BIGINT NOT NULL PRIMARY KEY, created_at DATETIME NOT NULL);
CREATE TABLE IF NOT EXISTS facebook_ads_campaign (id CHAR(36) NOT NULL PRIMARY KEY, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS product (id BIGINT NOT NULL PRIMARY KEY);
CREATE TABLE IF NOT EXISTS experiment (id BIGINT NOT NULL PRIMARY KEY);
CREATE TABLE IF NOT EXISTS business_process_chain_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, chain_code VARCHAR(100) NOT NULL,
 name VARCHAR(160) NOT NULL,purpose TEXT NOT NULL,outcome_description VARCHAR(500) NOT NULL,
 primary_metric VARCHAR(200) NOT NULL,version_number INT NOT NULL,status VARCHAR(20) NOT NULL,
 created_at DATETIME NOT NULL,published_at DATETIME,
 UNIQUE KEY uk_business_process_chain_code_version(chain_code,version_number));
CREATE TABLE IF NOT EXISTS business_process_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, process_code VARCHAR(100) NOT NULL,
 name VARCHAR(160) NOT NULL, purpose TEXT NOT NULL, owner_name VARCHAR(120) NOT NULL,
 trigger_description VARCHAR(500) NOT NULL, outcome_description VARCHAR(500) NOT NULL,
 version_number INT NOT NULL, status VARCHAR(20) NOT NULL, technical_reference VARCHAR(200),
 process_type VARCHAR(20) NOT NULL, execution_scope VARCHAR(32) NOT NULL, parent_process_code VARCHAR(100),
 diagram_json LONGTEXT NOT NULL, created_at DATETIME NOT NULL, published_at DATETIME,
 UNIQUE KEY uk_business_process_code_version(process_code,version_number)
);
CREATE TABLE IF NOT EXISTS business_process_activity_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, process_definition_id BIGINT NOT NULL,
 activity_id VARCHAR(100) NOT NULL, name VARCHAR(160) NOT NULL, objective TEXT,
 owner_name VARCHAR(160), execution_resource_code VARCHAR(100), subprocess_code VARCHAR(100),
 definition_json LONGTEXT NOT NULL, created_at DATETIME NOT NULL,
 UNIQUE KEY uk_business_process_activity_definition(process_definition_id,activity_id),
 FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
);
CREATE TABLE IF NOT EXISTS business_process_activity_instance (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, activity_definition_id BIGINT NOT NULL,
 source_reference VARCHAR(200) NOT NULL, occurrence_number INT NOT NULL, status VARCHAR(32) NOT NULL,
 entered_at DATETIME NOT NULL, exited_at DATETIME, objective_achieved BIT NOT NULL,
 objective_evidence_json LONGTEXT, blocked_reason LONGTEXT, known_cost_usd DECIMAL(18,8),
 cost_coverage VARCHAR(32) NOT NULL, evidence_quality VARCHAR(32) NOT NULL,
 created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
 UNIQUE KEY uk_business_process_activity_instance(activity_definition_id,source_reference,occurrence_number),
 FOREIGN KEY (activity_definition_id) REFERENCES business_process_activity_definition(id)
);
INSERT IGNORE INTO product (id) VALUES (91001),(91002);
INSERT IGNORE INTO experiment (id) VALUES (91001),(91002),(91003),(91004),(91005),(91006);
CREATE TABLE IF NOT EXISTS business_process_chain_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,chain_definition_id BIGINT NOT NULL,
 process_definition_id BIGINT NOT NULL,sequence_number INT NOT NULL,value_contribution VARCHAR(500) NOT NULL,
 created_at DATETIME NOT NULL,
 UNIQUE KEY uk_business_process_chain_sequence(chain_definition_id,sequence_number),
 UNIQUE KEY uk_business_process_chain_process(chain_definition_id,process_definition_id),
 FOREIGN KEY (chain_definition_id) REFERENCES business_process_chain_definition(id),
 FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id));
INSERT IGNORE INTO business_process_chain_definition
 (id,chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at)
 VALUES (91000,'pde-value-creation-delivery','Cadeia local de homologação','Gerar valor e vendas','Vendas entregues','Vendas líquidas',12,'PUBLISHED',NOW(),NOW());

INSERT IGNORE INTO business_process_definition (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (1,'pde-opportunity-discovery','Descoberta factual','Homologação segregada','Operador local','Teste local','Evidência local',6,'PUBLISHED','VALUE_PROCESS','PRODUCT','{"nodes": [{"id": "start", "type": "START", "label": "Início"}, {"id": "work", "type": "TASK", "label": "Descoberta factual", "owner": "Operador"}, {"id": "end", "type": "END", "label": "Fim"}], "flows": [{"from": "start", "to": "work"}, {"from": "work", "to": "end"}]}',NOW(),NOW());
INSERT IGNORE INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES (91000,1,1,'Valor de teste',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (1,'work','Descoberta factual','Teste local','Operador',NULL,'{"id": "work", "type": "TASK", "label": "Descoberta factual", "owner": "Operador"}',NOW());

INSERT IGNORE INTO business_process_definition (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (2,'pde-commercial-plan-offer','Estratégia e economia','Homologação segregada','Operador local','Teste local','Evidência local',6,'PUBLISHED','VALUE_PROCESS','PRODUCT','{"nodes": [{"id": "start", "type": "START", "label": "Início"}, {"id": "work", "type": "TASK", "label": "Estratégia e economia", "owner": "Operador"}, {"id": "end", "type": "END", "label": "Fim"}], "flows": [{"from": "start", "to": "work"}, {"from": "work", "to": "end"}]}',NOW(),NOW());
INSERT IGNORE INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES (91000,2,2,'Valor de teste',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (2,'work','Estratégia e economia','Teste local','Operador',NULL,'{"id": "work", "type": "TASK", "label": "Estratégia e economia", "owner": "Operador"}',NOW());

INSERT IGNORE INTO business_process_definition (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (3,'pde-construction-approval','Construção e aprovação local','Homologação segregada','Operador local','Teste local','Evidência local',8,'PUBLISHED','VALUE_PROCESS','PRODUCT','{"nodes": [{"id": "start", "type": "START", "label": "Início"}, {"id": "rework", "type": "TASK", "label": "Corrigir valor do produto", "owner": "Dédalo"}, {"id": "agentValidationGate", "type": "TASK", "label": "Gate multiagente", "owner": "Dédalo"}, {"id": "end", "type": "END", "label": "Fim"}], "flows": [{"from": "start", "to": "rework"}, {"from": "rework", "to": "agentValidationGate"}, {"from": "agentValidationGate", "to": "end"}]}',NOW(),NOW());
INSERT IGNORE INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES (91000,3,3,'Valor de teste',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (3,'rework','Corrigir valor do produto','Teste local','Dédalo',NULL,'{"id": "rework", "type": "TASK", "label": "Corrigir valor do produto", "owner": "Dédalo"}',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (3,'agentValidationGate','Gate multiagente','Teste local','Dédalo',NULL,'{"id": "agentValidationGate", "type": "TASK", "label": "Gate multiagente", "owner": "Dédalo"}',NOW());

INSERT IGNORE INTO business_process_definition (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (4,'pde-communication-sales-journey','Comunicação e jornada','Homologação segregada','Operador local','Teste local','Evidência local',7,'PUBLISHED','VALUE_PROCESS','PRODUCT','{"nodes": [{"id": "start", "type": "START", "label": "Início"}, {"id": "work", "type": "TASK", "label": "Comunicação e jornada", "owner": "Operador"}, {"id": "end", "type": "END", "label": "Fim"}], "flows": [{"from": "start", "to": "work"}, {"from": "work", "to": "end"}]}',NOW(),NOW());
INSERT IGNORE INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES (91000,4,4,'Valor de teste',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (4,'work','Comunicação e jornada','Teste local','Operador',NULL,'{"id": "work", "type": "TASK", "label": "Comunicação e jornada", "owner": "Operador"}',NOW());

INSERT IGNORE INTO business_process_definition (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (5,'pde-commercial-homologation-activation','Homologação e ativação','Homologação segregada','Operador local','Teste local','Evidência local',6,'PUBLISHED','VALUE_PROCESS','PRODUCT','{"nodes": [{"id": "start", "type": "START", "label": "Início"}, {"id": "work", "type": "TASK", "label": "Homologação e ativação", "owner": "Operador"}, {"id": "end", "type": "END", "label": "Fim"}], "flows": [{"from": "start", "to": "work"}, {"from": "work", "to": "end"}]}',NOW(),NOW());
INSERT IGNORE INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES (91000,5,5,'Valor de teste',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (5,'work','Homologação e ativação','Teste local','Operador',NULL,'{"id": "work", "type": "TASK", "label": "Homologação e ativação", "owner": "Operador"}',NOW());

INSERT IGNORE INTO business_process_definition (id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (6,'pde-sales-delivery-learning','Venda, entrega e aprendizado do PDE','Homologação segregada','Operador local','Teste local','Evidência local',4,'PUBLISHED','VALUE_PROCESS','PRODUCT','{"nodes": [{"id": "start", "type": "START", "label": "Ativação autorizada", "owner": "Backend", "description": "Orçamento e regras iniciam o ciclo."}, {"id": "optimization", "type": "TASK", "label": "Operar e otimizar o experimento", "owner": "Backend", "description": "Delega aquisição, mensuração e teste de variável ao subprocesso canônico.", "subprocessCode": "operacao-otimizacao-experimento"}, {"id": "delivery", "type": "TASK", "label": "Entregar cada venda e acompanhar satisfação", "owner": "Backend", "description": "Delega conciliação, entrega, suporte, satisfação e reembolso ao subprocesso canônico.", "subprocessCode": "venda-entrega-satisfacao-cliente"}, {"id": "consolidate", "type": "TASK", "label": "Consolidar resultado comercial", "owner": "Backend", "description": "Cruza vendas, receita, margem, entrega, satisfação e aprendizado sem reexecutar os subprocessos."}, {"id": "decision", "type": "GATEWAY", "label": "Qual decisão comercial?", "owner": "Backend e operador humano", "description": "Decide CONTINUAR, AJUSTAR, ESCALAR ou PARAR; tarefas e impacto estimado não contam como venda."}, {"id": "end", "type": "END", "label": "Ciclo decidido por vendas entregues", "owner": "Backend", "description": "A decisão e suas evidências ficam auditáveis."}], "flows": [{"from": "start", "to": "optimization"}, {"from": "start", "to": "delivery"}, {"from": "optimization", "to": "consolidate"}, {"from": "delivery", "to": "consolidate"}, {"from": "consolidate", "to": "decision"}, {"from": "decision", "to": "end"}]}',NOW(),NOW());
INSERT IGNORE INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES (91000,6,6,'Valor de teste',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (6,'optimization','Operar e otimizar o experimento','Teste local','Backend','operacao-otimizacao-experimento','{"id": "optimization", "type": "TASK", "label": "Operar e otimizar o experimento", "owner": "Backend", "description": "Delega aquisição, mensuração e teste de variável ao subprocesso canônico.", "subprocessCode": "operacao-otimizacao-experimento"}',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (6,'delivery','Entregar cada venda e acompanhar satisfação','Teste local','Backend','venda-entrega-satisfacao-cliente','{"id": "delivery", "type": "TASK", "label": "Entregar cada venda e acompanhar satisfação", "owner": "Backend", "description": "Delega conciliação, entrega, suporte, satisfação e reembolso ao subprocesso canônico.", "subprocessCode": "venda-entrega-satisfacao-cliente"}',NOW());
INSERT IGNORE INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,subprocess_code,definition_json,created_at) VALUES (6,'consolidate','Consolidar resultado comercial','Teste local','Backend',NULL,'{"id": "consolidate", "type": "TASK", "label": "Consolidar resultado comercial", "owner": "Backend", "description": "Cruza vendas, receita, margem, entrega, satisfação e aprendizado sem reexecutar os subprocessos."}',NOW());
