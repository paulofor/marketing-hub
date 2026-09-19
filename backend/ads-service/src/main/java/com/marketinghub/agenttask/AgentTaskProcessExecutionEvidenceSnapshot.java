package com.marketinghub.agenttask;

/**
 * Responsabilidade: transportar somente as provas necessárias para validar a última execução de
 * cada etapa comercial.
 */
public record AgentTaskProcessExecutionEvidenceSnapshot(
    Long taskId, String evidenceJson, String resultJson) {}
