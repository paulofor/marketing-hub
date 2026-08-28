package com.marketinghub.businessprocess.execution.service.agentactivity;

/** Responsabilidade: explicar se uma atividade de agente pode abrir uma nova tentativa. */
public record AgentProductProcessActivityReadiness(boolean ready, String reason) {}
