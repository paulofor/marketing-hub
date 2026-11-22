package com.marketinghub.businessprocess.automation.v1.service.status;

import com.marketinghub.agenttask.AgentTaskResultView;

/** Responsabilidade: identificar a execução que reserva a fila e sua pendência persistida. */
public record ProcessRunQueueBlocker(
    Long runId,
    Long processDefinitionId,
    String processName,
    Integer processVersion,
    Long chainId,
    Long learningCycleId,
    String sourceReference,
    String status,
    String reason,
    String currentActivityName,
    String navigationUrl) {
  /**
   * Converte motivos técnicos legados somente na projeção pública, sem alterar o estado persistido.
   */
  public ProcessRunQueueBlocker {
    reason = AgentTaskResultView.businessReason(reason);
  }
}
