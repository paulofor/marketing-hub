package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir exposição de falhas técnicas como explicação de negócio. */
class AgentTaskResultViewTest {
  /** Não transforma ausência de acompanhamento em entrega inexistente ou custo zero. */
  @Test
  void projectsLostLeaseWithoutClaimingModelFailureOrZeroCost() {
    var task = new AgentTask();
    task.setStatus("BLOCKED");
    task.setExecutionError("DEDALO_LEASE_RECOVERY_EXHAUSTED|internal diagnostic");
    assertThat(AgentTaskResultView.humanSummary(task).text())
        .contains("acompanhamento", "cobrança duplicada", "consumo")
        .doesNotContain("DEDALO_", "internal", "zero");
    assertThat(task.getExecutionError()).contains("internal diagnostic");
  }

  /** Projeta orientações legadas de contextos distintos sem alterar erro ou ação persistidos. */
  @Test
  void projectsLegacyGuidanceWithoutRestartAdviceOrInternalCode() {
    for (long id : java.util.List.of(8001L, 9001L)) {
      var task = new AgentTask();
      task.setId(id);
      task.setBlockerCategory("TECHNICAL_FAILURE");
      String raw =
          "Corrija a causa registrada e reinicie a tarefa: DEDALO_LEASE_RECOVERY_EXHAUSTED|diagnóstico";
      task.setBlockerAction(raw);
      task.setExecutionError("DEDALO_LEASE_RECOVERY_EXHAUSTED|diagnóstico");
      var full = AgentTaskAuditView.blockerGuidance(task);
      var compact =
          new AgentTaskBlockerGuidanceResponse("TECHNICAL_FAILURE", raw, java.util.List.of());
      assertThat(full.recommendedAction())
          .isEqualTo(compact.recommendedAction())
          .contains("conferir", "retorno original", "consumo")
          .doesNotContain("DEDALO_", "reinicie", "diagnóstico");
      assertThat(task.getBlockerAction()).isEqualTo(raw);
      assertThat(task.getExecutionError()).contains("DEDALO_");
    }
    assertThat(
            new AgentTaskBlockerGuidanceResponse(
                    "FUNCTIONAL_ADJUSTMENT", "Corrigir checkout", java.util.List.of())
                .recommendedAction())
        .isEqualTo("Corrigir checkout");
  }

  /** Explica interrupção legada sem inventar resultado ou consumo zero. */
  @Test
  void summarizesInterruptedContextWithoutRawError() {
    var task = new AgentTask();
    task.setStatus("BLOCKED");
    task.setExecutionError("com.internal.Exception: input_too_large {raw:true}");
    var summary = AgentTaskResultView.humanSummary(task);
    assertThat(summary.text())
        .contains("Nenhuma entrega", "consumo")
        .doesNotContain("Exception", "raw");
    task.setResultJson("{\"humanSummary\":\"Entrega preservada para recuperação do callback.\"}");
    assertThat(AgentTaskResultView.humanSummary(task).text()).contains("Entrega preservada");
  }

  /** Mantém bloqueios funcionais e normaliza somente as duas falhas de limite conhecidas. */
  @Test
  void preservesFunctionalReasonsAndProjectsBothContextFailures() {
    assertThat(AgentTaskResultView.businessReason(null)).isNull();
    assertThat(AgentTaskResultView.businessReason("Checkout ausente na evidência."))
        .isEqualTo("Checkout ausente na evidência.");
    for (String error : java.util.List.of("input_too_large", "PDE_CONTEXT_BUDGET_V1")) {
      assertThat(AgentTaskResultView.businessReason("ClasseInterna: " + error + " {raw: true}"))
          .contains("interrompida", "diagnóstico", "consumo")
          .doesNotContain(error, "ClasseInterna", "raw");
    }
  }
}
