package com.marketinghub.businessprocess.automation.v1.service.status;

/** Responsabilidade: explicar a condição ou decisão pendente e seu destino oficial. */
public record ProcessRunUserAction(
    String code,
    String title,
    String reason,
    String responsible,
    String actionLabel,
    String actionUrl,
    String afterAction,
    String evidenceReference) {
  /** Distingue insumos operacionais de uma decisão humana, preservando os demais contratos. */
  public String waitingStatus() {
    return "PREPARE_CYCLE_COMMERCIAL".equals(code) ? "WAITING_INPUT" : "WAITING_HUMAN";
  }
}
