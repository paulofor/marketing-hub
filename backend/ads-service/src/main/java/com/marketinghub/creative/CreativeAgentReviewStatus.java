package com.marketinghub.creative;

/**
 * Responsabilidade: representar o estado auditável da revisão de anúncio pelo agente especialista.
 */
public enum CreativeAgentReviewStatus {
  PENDING,
  PROCESSING,
  APPROVED,
  ADJUST,
  REJECTED,
  FAILED
}
