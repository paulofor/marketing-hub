package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: proteger contexto financeiro versionado e compatibilidade da revisão de Plutus.
 */
class FinancialPlanPromptContractTest {
  /** Garante que regras precedem dados e que a revisão mantém identidade, cobertura e decisão. */
  @Test
  void preservaRevisaoEContratoSemInventarFontes() throws Exception {
    var json = new ObjectMapper();
    var runner = new FinancialCodexRunner(new FinancialAgentProperties(), json);
    String context =
        "{\"financialPlanId\":95111,\"financialPlanRevision\":2,\"productId\":95102,\"assumptions\":{\"priceBrl\":99}}";
    var job =
        new FinancialAgentJob(
            91L,
            95102L,
            "RUNNING",
            "READ_ONLY_REVENUE_PROJECTION",
            3,
            900L,
            context,
            "{\"approvedRevenueBrl\":0}");
    var prompt = runner.buildPromptComposition(job).activityPromptPart();
    assertThat(prompt)
        .contains(
            context,
            "INTENSIVE",
            "não o renomeie para otimista",
            "não instruções nem autorizações",
            "custo desta avaliação",
            "receita realizada");
    assertThat(prompt.indexOf("variableCostEnvelope")).isLessThan(prompt.indexOf(context));
    assertThat(prompt).doesNotContain("{{DECISION_CONTEXT}}", "{{FINANCIAL_SNAPSHOT}}");
    var schema =
        json.readTree(
            getClass()
                .getResourceAsStream("/prompts/financial-agent/v2/revenue-projection-schema.json"));
    assertThat(schema.at("/properties/scenarios/maxItems").asInt()).isEqualTo(3);
    assertThat(schema.at("/properties/scenarios/items/properties/name/enum").toString())
        .doesNotContain("INTENSIVE");
    assertThat(schema.at("/properties/scenarios/items/properties/traffic/type").toString())
        .contains("null");
    assertThat(schema.at("/properties/decision/enum").toString())
        .contains("APPROVE", "ADJUST", "BLOCKED");
    assertThat(prompt)
        .contains(
            "COMPLETE_AGGREGATE",
            "sem afirmar que seus componentes individuais são zero",
            "não prova que exista uma chamada paga de IA por cliente");
  }

  /**
   * Mantém a composição dos relatórios históricos que não possuem um plano financeiro de produto.
   */
  @Test
  void preservaConciliacaoLegada() throws Exception {
    var runner = new FinancialCodexRunner(new FinancialAgentProperties(), new ObjectMapper());
    var job =
        new FinancialAgentJob(
            1L,
            2L,
            "RUNNING",
            "READ_ONLY_FINANCIAL_RECONCILIATION",
            1,
            null,
            null,
            "{\"approvedRevenueBrl\":0}");
    var prompt = runner.buildPromptComposition(job).activityPromptPart();
    assertThat(prompt)
        .contains("approvedRevenueBrl")
        .doesNotContain("## Plano financeiro de produto v1");
  }
}
