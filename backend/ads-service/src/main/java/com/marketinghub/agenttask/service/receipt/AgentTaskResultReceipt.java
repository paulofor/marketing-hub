package com.marketinghub.agenttask.service.receipt;

/** Responsabilidade: confirmar estado e bytes do callback sem expor o conteúdo preservado. */
public record AgentTaskResultReceipt(
    Long taskId, String status, String resultSha256, String evidenceSha256) {}
