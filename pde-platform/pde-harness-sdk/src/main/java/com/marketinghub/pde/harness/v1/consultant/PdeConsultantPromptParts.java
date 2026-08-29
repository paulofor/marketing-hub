package com.marketinghub.pde.harness.v1.consultant;

/** Mantém separadas e auditáveis as partes estável e específica do prompt do consultor. */
public record PdeConsultantPromptParts(
    String agentPart,
    String agentVersion,
    String activityPart,
    String activityVersion,
    String customerMessage) {

  /** Valida as duas partes, suas versões e a mensagem atual antes da composição. */
  public PdeConsultantPromptParts {
    agentPart = requireText(agentPart, "agentPart", 40_000);
    agentVersion = requireText(agentVersion, "agentVersion", 128);
    activityPart = requireText(activityPart, "activityPart", 40_000);
    activityVersion = requireText(activityVersion, "activityVersion", 128);
    customerMessage = requireText(customerMessage, "customerMessage", 20_000);
  }

  /** Valida texto obrigatório, remove espaços externos e limita o contexto do turno. */
  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " excede " + maxLength + " caracteres");
    }
    return normalized;
  }
}
