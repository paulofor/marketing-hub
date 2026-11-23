package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: preservar pareceres e apresentar sínteses sem nova inferência ou falsa
 * aprovação.
 */
class AgentTaskHumanSummaryTest {
  /** O texto novo do agente é exibido sem alterar os bytes e a decisão do parecer. */
  @Test
  void exposesGeneratedSummaryWithoutRewritingHistory() {
    var task =
        task(
            "{\"decision\":\"BLOCKED\",\"humanSummary\":\"Falta demonstrar a recuperação. Psique deve testar novamente.\"}");
    String original = task.getResultJson();
    assertThat(AgentTaskResultView.humanSummary(task))
        .isEqualTo(
            new AgentTaskHumanSummary(
                "Falta demonstrar a recuperação. Psique deve testar novamente.",
                "AGENT",
                "BLOCKED"));
    assertThat(task.getResultJson()).isEqualTo(original);
  }

  /** Lacuna determinística histórica não vira defeito comprovado nem aprovação após suplemento. */
  @Test
  void explainsLegacyEvidenceGapWithoutApprovingIt() {
    var task =
        task(
            """
        {"contractVersion":"PDE_TEMIS_AGENT_VALIDATION_V1","decision":"BLOCKED",
        "agentValidationChecks":{"technicalHarnessPassed":false,"privacyPreserved":true,
        "sameProductAndVersion":true,"criteriaPredeclared":true,"threeScenarioReviewsApproved":true,
        "syntheticEvidenceLabeled":true,"internalTrafficSegregated":true,"paymentDisabled":true,
        "publicationDisabled":true,"campaignDisabled":true,"zeroMediaSpend":true,
        "noHumanOrCommercialClaim":true,"strategyFidelity":true},
        "commercialRationale":"A cobertura determinística permanece incompleta."}
        """);
    assertThat(AgentTaskResultView.humanSummary(task).text())
        .contains("provas adicionais", "nova revisão");
    assertThat(AgentTaskResultView.humanSummary(task).source()).isEqualTo("DETERMINISTIC");
    task.setResultJson(
        task.getResultJson().replace("\"privacyPreserved\":true", "\"privacyPreserved\":false"));
    assertThat(AgentTaskResultView.humanSummary(task).source()).isEqualTo("LEGACY_EXCERPT");
  }

  /**
   * Texto legado conserva origem, limite e conclusão válida sem fingir síntese feita pelo agente.
   */
  @Test
  void boundsLegacyTextAndPreservesApprovedPath() {
    var task =
        task("{\"decision\":\"APPROVED\",\"summary\":\"" + "Texto original. ".repeat(100) + "\"}");
    var summary = AgentTaskResultView.humanSummary(task);
    assertThat(summary.text().length()).isLessThanOrEqualTo(420);
    assertThat(summary.source()).isEqualTo("LEGACY_EXCERPT");
    assertThat(summary.decision()).isEqualTo("APPROVED");
  }

  /** Contratos fixados no catálogo usam a síntese existente sem alterar schema nem parecer. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"customerPerspective", "commercialRationale"})
  void readsCatalogSummaryWithoutContractMigration(String field) {
    var task =
        task(
            "{\"decision\":\"ADJUST\",\""
                + field
                + "\":\"Falta demonstrar a recuperação. O responsável deve completar a prova.\"}");
    String original = task.getResultJson();
    var summary = AgentTaskResultView.humanSummary(task);
    assertThat(summary.text()).contains("Falta demonstrar a recuperação");
    assertThat(summary.source()).isEqualTo("LEGACY_EXCERPT");
    assertThat(summary.decision()).isEqualTo("ADJUST");
    assertThat(task.getResultJson()).isEqualTo(original);
  }

  /** Dados ausentes ou inválidos mantêm ausência de síntese, sem inventar conclusão. */
  @Test
  void invalidOrMissingResultHasNoSummary() {
    assertThat(AgentTaskResultView.humanSummary(task("invalid"))).isNull();
    assertThat(AgentTaskResultView.humanSummary(task("{}"))).isNull();
    assertThat(AgentTaskResultView.humanSummary(null)).isNull();
  }

  /** Uma resposta READY recusada não aparece como entrega aceita ou erro bruto ao usuário. */
  @Test
  void rejectedReadyResultIsPresentedAsRecoverableBlock() {
    var task =
        task("{\"decision\":\"READY\",\"humanSummary\":\"Componentes preparados com sucesso.\"}");
    task.setStatus("BLOCKED");
    task.setExecutionError(
        AgentTaskResultRejectedException.CODE + "|java.lang.IllegalArgumentException: detalhe");
    var summary = AgentTaskResultView.humanSummary(task);
    assertThat(summary.decision()).isEqualTo("BLOCKED");
    assertThat(summary.text())
        .contains("preservada", "sem nova chamada", "não comprova implementação")
        .doesNotContain("IllegalArgumentException", AgentTaskResultRejectedException.CODE);
  }

  /** Cria somente uma tarefa local, sem banco, custo, callback ou agente externo. */
  private AgentTask task(String result) {
    var task = new AgentTask();
    task.setId(99021L);
    task.setResultJson(result);
    return task;
  }
}
