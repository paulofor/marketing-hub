package com.marketinghub.agenttask;

/**
 * Responsabilidade: ler somente o resultado da tarefa exata para sintetizar sem carregar prompts.
 */
public record AgentTaskHumanSummaryInputSnapshot(Long taskId, String resultJson) {}
