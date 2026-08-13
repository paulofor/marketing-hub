package com.marketinghub.agentmonitor;

/** Responsabilidade: receber o resultado técnico do controlador de implantação. */
public record AgentExecutorAdminCompletionRequest(boolean success, String detail) {}
