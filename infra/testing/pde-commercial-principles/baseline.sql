CREATE TABLE business_process_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 process_code VARCHAR(100) NOT NULL, name VARCHAR(160) NOT NULL, purpose TEXT NOT NULL,
 owner_name VARCHAR(120) NOT NULL, trigger_description VARCHAR(500) NOT NULL,
 outcome_description VARCHAR(500) NOT NULL, version_number INT NOT NULL,
 status VARCHAR(20) NOT NULL, technical_reference VARCHAR(200), process_type VARCHAR(20) NOT NULL,
 parent_process_code VARCHAR(100), execution_scope VARCHAR(32) NOT NULL,
 diagram_json LONGTEXT NOT NULL, created_at DATETIME NOT NULL, published_at DATETIME,
 UNIQUE KEY uk_process (process_code,version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=4001;
CREATE TABLE business_process_activity_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, process_definition_id BIGINT NOT NULL,
 activity_id VARCHAR(100) NOT NULL, name VARCHAR(160) NOT NULL, objective TEXT,
 owner_name VARCHAR(160), execution_resource_code VARCHAR(100), subprocess_code VARCHAR(100),
 definition_json LONGTEXT NOT NULL, created_at DATETIME NOT NULL,
 UNIQUE KEY uk_activity(process_definition_id,activity_id),
 FOREIGN KEY(process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE business_process_chain_definition (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, chain_code VARCHAR(100) NOT NULL,
 name VARCHAR(160) NOT NULL, purpose TEXT NOT NULL, outcome_description VARCHAR(500) NOT NULL,
 primary_metric VARCHAR(200) NOT NULL, version_number INT NOT NULL, status VARCHAR(20) NOT NULL,
 created_at DATETIME NOT NULL, published_at DATETIME,
 UNIQUE KEY uk_chain(chain_code,version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=6001;
CREATE TABLE business_process_chain_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, chain_definition_id BIGINT NOT NULL,
 process_definition_id BIGINT NOT NULL, sequence_number INT NOT NULL,
 value_contribution VARCHAR(500) NOT NULL, created_at DATETIME NOT NULL,
 UNIQUE KEY uk_chain_sequence(chain_definition_id,sequence_number),
 UNIQUE KEY uk_chain_process(chain_definition_id,process_definition_id),
 FOREIGN KEY(chain_definition_id) REFERENCES business_process_chain_definition(id),
 FOREIGN KEY(process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE product (id BIGINT PRIMARY KEY, chain_definition_id BIGINT NOT NULL,
 FOREIGN KEY(chain_definition_id) REFERENCES business_process_chain_definition(id));
CREATE TABLE agent_task (id BIGINT PRIMARY KEY, process_definition_id BIGINT NOT NULL,
 source_reference VARCHAR(100) NOT NULL, status VARCHAR(30) NOT NULL,
 estimated_cost_usd DECIMAL(18,8), result_json LONGTEXT,
 FOREIGN KEY(process_definition_id) REFERENCES business_process_definition(id));
