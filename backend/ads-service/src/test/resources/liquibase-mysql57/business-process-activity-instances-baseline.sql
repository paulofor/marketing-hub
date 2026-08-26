CREATE TABLE business_process_definition (
  id BIGINT NOT NULL AUTO_INCREMENT,
  diagram_json LONGTEXT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent (
  id BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  assigned_agent_id BIGINT NOT NULL,
  source_reference VARCHAR(200) NULL,
  process_definition_id BIGINT NULL,
  process_activity_id VARCHAR(100) NULL,
  process_activity_name VARCHAR(160) NULL,
  status VARCHAR(30) NOT NULL,
  execution_error LONGTEXT NULL,
  estimated_cost_usd DECIMAL(18,8) NULL,
  cost_estimation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REPORTED',
  delivered_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_fixture_agent_task_assignee
    FOREIGN KEY (assigned_agent_id) REFERENCES agent(id),
  CONSTRAINT fk_fixture_agent_task_process
    FOREIGN KEY (process_definition_id) REFERENCES business_process_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO business_process_definition (id, diagram_json, created_at)
VALUES (
  9,
  '{"nodes":[{"id":"start","type":"START","label":"Início"},{"id":"html","type":"TASK","label":"Montar HTML","description":"Entregar HTML funcional","owner":"Dédalo"},{"id":"review","type":"TASK","label":"Revisar experiência","description":"Comprovar a experiência","owner":"Psique"},{"id":"end","type":"END","label":"Fim"}],"flows":[{"from":"start","to":"html"},{"from":"html","to":"review"},{"from":"review","to":"end"}]}',
  '2026-08-20 10:00:00'
);

INSERT INTO agent (id) VALUES (7), (8);

INSERT INTO agent_task (
  id, assigned_agent_id, source_reference, process_definition_id, process_activity_id, process_activity_name,
  status, execution_error, estimated_cost_usd, cost_estimation_status,
  delivered_at, created_at, updated_at
) VALUES
  (1, 7, 'experiment:88', 9, 'html', 'Montar HTML', 'BLOCKED', 'Primeira tentativa inválida', 0.10000000, 'ESTIMATED', NULL, '2026-08-20 10:05:00', '2026-08-20 10:20:00'),
  (2, 7, 'experiment:88', 9, 'html', 'Montar HTML', 'COMPLETED', NULL, 0.20000000, 'ESTIMATED', '2026-08-20 10:35:00', '2026-08-20 10:21:00', '2026-08-20 10:35:00'),
  (3, 8, 'experiment:88', 9, 'review', 'Revisar experiência', 'BLOCKED', 'Prova insuficiente', NULL, 'NOT_REPORTED', NULL, '2026-08-20 10:36:00', '2026-08-20 10:50:00'),
  (4, 7, 'legacy:4', NULL, NULL, NULL, 'PENDING', NULL, NULL, 'NOT_REPORTED', NULL, '2026-08-20 11:00:00', '2026-08-20 11:00:00');
