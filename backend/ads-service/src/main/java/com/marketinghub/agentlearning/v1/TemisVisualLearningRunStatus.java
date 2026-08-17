package com.marketinghub.agentlearning.v1;

/** Responsabilidade: representar o estado auditável de uma consolidação visual de Têmis. */
public enum TemisVisualLearningRunStatus {
  PENDING,
  PROCESSING,
  READY_FOR_PROMOTION,
  REJECTED,
  PROMOTED,
  ROLLED_BACK,
  FAILED
}
