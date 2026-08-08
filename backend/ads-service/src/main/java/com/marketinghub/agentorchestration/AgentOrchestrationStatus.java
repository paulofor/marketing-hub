package com.marketinghub.agentorchestration;

/** Responsabilidade: representar o estado consolidado de uma coordenacao entre agentes. */
public enum AgentOrchestrationStatus {
  WAITING_FOR_AGENTS,
  BLOCKED,
  READY_FOR_HUMAN_DECISION
}
