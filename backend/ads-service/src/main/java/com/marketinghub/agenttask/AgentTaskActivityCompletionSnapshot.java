package com.marketinghub.agenttask;

/**
 * Responsabilidade: transportar a última conclusão de uma atividade sem carregar o prompt da
 * tarefa.
 */
public record AgentTaskActivityCompletionSnapshot(
    Long taskId, String evidenceJson, String resultJson) {}
