package com.marketinghub.agenttask;

/**
 * Responsabilidade: expor a tentativa vigente da revisão sem carregar prompts ou auditoria técnica.
 */
public record AgentTaskReviewSnapshot(
    Long taskId, String status, String evidenceJson, String resultJson) {}
