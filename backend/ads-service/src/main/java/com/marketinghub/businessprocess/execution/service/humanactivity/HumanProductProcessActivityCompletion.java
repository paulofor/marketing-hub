package com.marketinghub.businessprocess.execution.service.humanactivity;

import java.util.Map;

/**
 * Responsabilidade: transportar o resultado funcional calculado por um gate humano especializado.
 */
public record HumanProductProcessActivityCompletion(
    boolean objectiveAchieved,
    String message,
    String blockedReason,
    Map<String, Object> structuredEvidence) {

  /** Cria a conclusão padrão de uma aprovação humana que atingiu seu objetivo. */
  public static HumanProductProcessActivityCompletion completed(Map<String, Object> evidence) {
    return new HumanProductProcessActivityCompletion(
        true, "Decisão humana aprovada e registrada com evidência auditável.", null, evidence);
  }

  /** Cria uma tentativa registrada que não atingiu os critérios e pode ser repetida. */
  public static HumanProductProcessActivityCompletion blocked(
      String message, String blockedReason, Map<String, Object> evidence) {
    return new HumanProductProcessActivityCompletion(false, message, blockedReason, evidence);
  }

  /** Preserva a evidência calculada como snapshot imutável. */
  public HumanProductProcessActivityCompletion {
    structuredEvidence = structuredEvidence == null ? Map.of() : Map.copyOf(structuredEvidence);
  }
}
