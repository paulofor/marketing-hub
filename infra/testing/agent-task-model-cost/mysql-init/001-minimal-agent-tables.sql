CREATE TABLE agent_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  execution_error LONGTEXT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE gera_landing_stage_execution (
  id BIGINT NOT NULL AUTO_INCREMENT,
  input_tokens INT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;
