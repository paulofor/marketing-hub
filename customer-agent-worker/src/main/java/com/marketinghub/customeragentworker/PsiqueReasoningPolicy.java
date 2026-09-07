package com.marketinghub.customeragentworker;

/** Responsabilidade: impor o esforço máximo de raciocínio em toda execução de Psique. */
final class PsiqueReasoningPolicy {
  static final String MAXIMUM_EFFORT = "max";

  /** Impede instanciação de uma política composta somente por contratos estáticos. */
  private PsiqueReasoningPolicy() {}

  /** Valida e normaliza a configuração, recusando qualquer redução silenciosa de qualidade. */
  static String requireMaximum(String value) {
    if (!MAXIMUM_EFFORT.equals(value == null ? null : value.trim())) {
      throw new IllegalStateException(
          "CUSTOMER_AGENT_REASONING_EFFORT deve ser max em toda execução de Psique.");
    }
    return MAXIMUM_EFFORT;
  }

  /** Monta a configuração canônica aceita pela CLI do Codex. */
  static String codexConfiguration(String value) {
    return "model_reasoning_effort=\"" + requireMaximum(value) + "\"";
  }
}
