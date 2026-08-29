CREATE TABLE agent_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status VARCHAR(30) NOT NULL,
  execution_model_code VARCHAR(128) NULL,
  execution_reasoning_effort VARCHAR(32) NULL,
  execution_prompt LONGTEXT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO agent_task
  (id, status, execution_model_code, execution_reasoning_effort, execution_prompt,
   created_at, updated_at)
VALUES
  (258, 'BLOCKED', 'gpt-5.6-sol', NULL, 'Prompt integral legado da tarefa 258.',
   '2026-08-28 16:15:48', '2026-08-28 16:16:49'),
  (259, 'IN_PROGRESS', NULL, NULL, NULL,
   '2026-08-29 01:00:00', '2026-08-29 01:00:00');
