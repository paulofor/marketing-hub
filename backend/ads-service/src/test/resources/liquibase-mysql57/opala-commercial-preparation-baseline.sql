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
  execution_scope VARCHAR(32) NOT NULL DEFAULT 'PRODUCT',
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


INSERT INTO business_process_definition (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,diagram_json,created_at) VALUES ('pde-sales-delivery-learning','Vendas histórico','Fixture','Fixture','Fixture','Fixture',6,'PUBLISHED','{"nodes":[],"flows":[]}',UTC_TIMESTAMP());
INSERT INTO business_process_definition (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,diagram_json,created_at) VALUES ('pde-commercial-homologation-activation','Homologação comercial','Fixture','Fixture','Fixture','Fixture',6,'PUBLISHED','{"nodes":[{"id":"start","type":"START"},{"id":"end","type":"END"}],"flows":[{"from":"start","to":"end"}]}',UTC_TIMESTAMP());
INSERT INTO business_process_chain_definition (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at) VALUES ('pde-value-creation-delivery','Cadeia histórica','Fixture','Fixture','Vendas',14,'PUBLISHED',UTC_TIMESTAMP());
INSERT INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) SELECT c.id,p.id,5,'Fixture',UTC_TIMESTAMP() FROM business_process_chain_definition c JOIN business_process_definition p ON p.process_code='pde-commercial-homologation-activation';
INSERT INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) SELECT c.id,p.id,6,'Fixture',UTC_TIMESTAMP() FROM business_process_chain_definition c JOIN business_process_definition p ON p.process_code='pde-sales-delivery-learning';
