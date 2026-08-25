package com.marketinghub.agent.integration;

import java.time.Instant;

/** Responsabilidade: transportar a última execução conhecida do workflow de um agente. */
public record AgentWorkflowFreshness(
    Instant lastWorkflowRunAt,
    String workflowName,
    String workflowFile,
    String workflowConclusion,
    String workflowUrl) {}
