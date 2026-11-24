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

  /** Projeta tarefa e processo pai de contextos distintos sem alterar erro ou ação persistidos. */
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
      var parent =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .convertValue(
                  java.util.Map.of("id", id, "reason", "Tarefa anterior: " + raw),
                  com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunResponse
                      .class);
      assertThat(parent.reason()).isEqualTo(full.recommendedAction());
      assertThat(parent.id()).isEqualTo(id);
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

  /**
   * A falha legada do harness permanece auditada, mas nunca vaza stack trace no contrato público.
   */
  @org.junit.jupiter.api.Test
  void projectsLegacyHarnessFailureAsRecoverableBusinessMessage() {
    var task = new AgentTask();
    task.setId(99130L);
    task.setStatus("BLOCKED");
    task.setBlockerCategory("EXECUTOR_FAILURE");
    String raw =
        "com.marketinghub.customeragentworker.PdeAgentValidationHarnessRunner$HarnessException: Falha no harness multiagente: node:internal\nAssertionError HTTP 409 at file:///app/browser/musa-continuous-harness.mjs:29";
    task.setExecutionError(raw);
    assertThat(AgentTaskResultView.businessReason(raw))
        .contains("recuperar o acesso", "Não há parecer funcional")
        .doesNotContain("Exception", "file:", "node:", "409");
    assertThat(AgentTaskResultView.humanSummary(task).text()).doesNotContain("Exception", "file:");
    assertThat(task.getExecutionError()).isEqualTo(raw);
  }

  /**
   * A lista compacta protege o histórico inteiro; a auditoria não é confundida com mensagem
   * pública.
   */
  @org.junit.jupiter.api.Test
  void compactActivityHistoryDoesNotExposeHarnessStack() {
    var task = new AgentTask();
    task.setId(99130L);
    task.setStatus("BLOCKED");
    task.setProcessDefinition(new com.marketinghub.businessprocess.BusinessProcessDefinition());
    task.setAssignedAgent(new com.marketinghub.agent.Agent());
    task.setExecutionError(
        "com.marketinghub.customeragentworker.PdeAgentValidationHarnessRunner$HarnessException: Falha no harness multiagente: node:internal stack file:///app/browser/harness.mjs");
    var service =
        org.mockito.Mockito.mock(
            com.marketinghub.businessprocess.execution.service
                .BusinessProcessActivityExecutionService.class,
            org.mockito.Mockito.CALLS_REAL_METHODS);
    com.marketinghub
            .businessprocess
            .execution
            .service
            .recentExecutions
            .BusinessProcessActivityExecutionResponse
        view =
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "response", task, "Fixture", false);
    assertThat(view.executionError())
        .contains("recuperar o acesso")
        .doesNotContain("Exception", "file:", "node:");
    assertThat(view.promptSent()).isNull();
    assertThat(view.comments()).isNull();
    assertThat(task.getExecutionError()).contains("HarnessException");
  }
}
