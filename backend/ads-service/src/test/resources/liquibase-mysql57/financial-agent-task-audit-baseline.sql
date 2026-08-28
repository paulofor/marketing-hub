CREATE TABLE agent_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status VARCHAR(30) NOT NULL,
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
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE financial_agent_execution (
  id BIGINT NOT NULL AUTO_INCREMENT,
  commercial_plan_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  authority_mode VARCHAR(64) NOT NULL,
  commercial_plan_version INT NOT NULL,
  agent_task_id BIGINT NULL,
  projection_request LONGTEXT NULL,
  financial_snapshot LONGTEXT NOT NULL,
  reconciliation_json LONGTEXT NULL,
  daily_report LONGTEXT NULL,
  raw_model_response LONGTEXT NULL,
  model VARCHAR(191) NULL,
  estimated_cost DECIMAL(12,4) NULL,
  error_message TEXT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO agent_task
  (id, status, received_at, delivered_at, result_json, evidence_json, execution_error,
   input_tokens, cached_input_tokens, output_tokens, estimated_cost_usd,
   cost_estimation_status, model_usage_updated_at, execution_model_code,
   execution_reasoning_effort, execution_prompt, created_at, updated_at)
VALUES
  (253, 'COMPLETED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   'NOT_REPORTED', NULL, NULL, NULL, NULL, '2026-08-28 14:49:00', '2026-08-28 14:51:23'),
  (254, 'BLOCKED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
   'NOT_REPORTED', NULL, NULL, NULL, NULL, '2026-08-28 15:00:00', '2026-08-28 15:01:00'),
  (255, 'COMPLETED', '2026-08-28 16:00:00', '2026-08-28 16:02:00',
   '{"preserved":true}', '{"origin":"existing"}', NULL, 10, 2, 3, 0.01000000,
   'ESTIMATED', '2026-08-28 16:02:00', 'existing-model', 'high', 'existing prompt',
   '2026-08-28 16:00:00', '2026-08-28 16:02:00');

INSERT INTO financial_agent_execution
  (id, commercial_plan_id, status, authority_mode, commercial_plan_version, agent_task_id,
   projection_request, financial_snapshot, reconciliation_json, daily_report,
   raw_model_response, model, estimated_cost, error_message, started_at, finished_at,
   created_at, updated_at)
VALUES
  (27, 4, 'COMPLETED', 'READ_ONLY_REVENUE_PROJECTION', 3, 253, NULL,
   '{"planId":4,"approvedRevenueBrl":0}',
   '{"decision":"BLOCKED_BY_MISSING_SOURCE"}',
   'Sem tráfego real para estimar CAC.',
   '{"decision":"BLOCKED_BY_MISSING_SOURCE"}', 'gpt-5.6-sol', 0.3770, NULL,
   '2026-08-28 14:50:09', '2026-08-28 14:51:23',
   '2026-08-28 14:49:00', '2026-08-28 14:51:23'),
  (28, 4, 'FAILED', 'READ_ONLY_REVENUE_PROJECTION', 3, 254, NULL,
   '{"planId":4,"approvedRevenueBrl":0}', NULL, NULL, NULL, NULL, NULL,
   'MCP indisponível', '2026-08-28 15:00:10', '2026-08-28 15:01:00',
   '2026-08-28 15:00:00', '2026-08-28 15:01:00'),
  (29, 4, 'COMPLETED', 'READ_ONLY_REVENUE_PROJECTION', 3, 255, NULL,
   '{"planId":4}', '{"new":true}', 'Novo relatório', '{"new":true}',
   'new-model', 0.0200, NULL, '2026-08-28 16:00:10', '2026-08-28 16:02:00',
   '2026-08-28 16:00:00', '2026-08-28 16:02:00');
