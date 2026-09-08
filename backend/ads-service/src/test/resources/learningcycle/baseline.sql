CREATE TABLE IF NOT EXISTS agent_task (id BIGINT NOT NULL PRIMARY KEY, created_at DATETIME NOT NULL);
CREATE TABLE IF NOT EXISTS facebook_ads_campaign (id CHAR(36) NOT NULL PRIMARY KEY, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS product (id BIGINT NOT NULL PRIMARY KEY);
CREATE TABLE IF NOT EXISTS experiment (id BIGINT NOT NULL PRIMARY KEY);
CREATE TABLE IF NOT EXISTS business_process_chain_definition (id BIGINT NOT NULL PRIMARY KEY);
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
INSERT IGNORE INTO business_process_chain_definition (id) VALUES (91001);
