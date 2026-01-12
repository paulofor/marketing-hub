--liquibase formatted sql
--changeset repo:2031-06-10-agent-tables dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'agent_theme';
CREATE TABLE agent_theme (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_agent_theme_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    theme_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    execution_mode VARCHAR(50) NOT NULL,
    description LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_theme FOREIGN KEY (theme_id) REFERENCES agent_theme(id)
) ENGINE=InnoDB;
CREATE INDEX idx_agent_theme_id ON agent(theme_id);

CREATE TABLE agent_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    input_type VARCHAR(100) NULL,
    description LONGTEXT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_input_agent FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_agent_input_agent_id ON agent_input(agent_id);

CREATE TABLE agent_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    output_type VARCHAR(100) NULL,
    description LONGTEXT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_output_agent FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_agent_output_agent_id ON agent_output(agent_id);

CREATE TABLE agent_internal_function (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    function_type VARCHAR(100) NULL,
    description LONGTEXT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_internal_function_agent FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX idx_agent_internal_function_agent_id ON agent_internal_function(agent_id);
