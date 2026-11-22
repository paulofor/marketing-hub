package com.marketinghub.agenttask;

/** Responsabilidade: ler resultado e interrupção da tarefa exata sem carregar prompts ou provas. */
public record AgentTaskHumanSummaryInputSnapshot(
    Long taskId, String resultJson, String status, String executionError) {}
