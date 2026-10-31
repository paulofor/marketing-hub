package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.FinancialTaskSnapshot;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.getplan.PlanEvaluation;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: proteger resumo numérico, validade e origem sem inferência ou gasto. */
class LearningCycleFinancialAdviceTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningCycleJson json = new LearningCycleJson(new ObjectMapper());
  private final LearningCycleFinancialAdvice advice = new LearningCycleFinancialAdvice(tasks, json);
  private final Instant now = Instant.parse("2026-10-31T01:00:00Z");

  /** Monta identidade exata sem criar autorizações financeiras. */
  private LearningSalesCycle cycle(long product) {
    var cycle = new LearningSalesCycle();
    cycle.setProductId(product);
    cycle.setId(product + 12);
    cycle.setChainDefinitionId(27L);
    cycle.setExperimentId(product + 105);
    cycle.setVersionChangedAt(now.minusSeconds(3600));
    return cycle;
  }

  /** Representa o contrato aceito de planejamento, com números históricos de Capella. */
  private String result() {
    return """
        {"mode":"AGENT_VALIDATION_HYPOTHESIS","decision":"APPROVE",
         "economics":{"offerPriceBrl":67,"variableCostPerSaleBrl":41.74,
                      "contributionPerSaleBrl":25.26},
         "rationale":"Aprovo somente o desenho interno. Ainda não autoriza venda ou anúncios."}
        """;
  }

  /** Configura a última tentativa consultada, sem esconder pendência atrás de aprovação antiga. */
  private void latest(String status, boolean accepted, String payload) {
    when(tasks.findFinancialAdviceSnapshots(anyString(), any()))
        .thenReturn(
            List.of(new FinancialTaskSnapshot(883L, 126L, status, now, now, accepted, payload)));
  }

  /** Reproduz Capella e outra identidade: estratégia posterior invalida uso atual do conselho. */
  @ParameterizedTest
  @ValueSource(longs = {7, 907})
  void showsPlanningNumbersButRequiresReviewAfterNewStrategy(long product) {
    latest("COMPLETED", true, result());
    var cycle = cycle(product);
    when(tasks.existsBySourceReferenceAndProcessDefinitionIdAndProcessActivityIdAndCreatedAtAfter(
            "experiment:" + cycle.getExperimentId(), 126L, "marketStrategy", now))
        .thenReturn(true);
    var view = advice.read(cycle, null, "/financial/plans");
    assertThat(view.status()).isEqualTo("STALE");
    assertThat(view.recommendation()).contains("Atualize");
    assertThat(view.sourceId()).isEqualTo(883);
    assertThat(view.sourceUrl()).contains("products/" + product, "#activity-economics");
    assertThat(view.figures())
        .extracting(f -> f.value())
        .containsExactly(new BigDecimal("67"), new BigDecimal("41.74"), new BigDecimal("25.26"));
    assertThat(view.caution()).contains("não desconta").contains("não é lucro líquido");
    verify(tasks).findFinancialAdviceSnapshots(eq("experiment:" + cycle.getExperimentId()), any());
    verify(tasks, never()).save(any());
  }

  /** Um parecer válido de preparação permanece útil sem virar autorização comercial. */
  @Test
  void preservesValidPreparationWithoutCommercialApproval() {
    latest("COMPLETED", true, result());
    var view = advice.read(cycle(7), null, "/plan");
    assertThat(view.status()).isEqualTo("PREPARATION_ONLY");
    assertThat(view.recommendation()).contains("somente", "ainda precisam");
  }

  /**
   * Parecer negativo vigente orienta corrigir a economia, sem sugerir mera repetição da análise.
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADJUST", "REJECT"})
  void preservesCurrentNegativeDecision(String decision) {
    latest("BLOCKED", false, result().replace("APPROVE", decision));
    var view = advice.read(cycle(7), null, "/plan");
    assertThat(view.status()).isEqualTo(decision);
    assertThat(view.figures()).hasSize(3);
    assertThat(view.fullRationale()).isNotBlank();
  }

  /** Resultado favorável sem aceite exige recuperar a passagem, sem atribuir ajustes a Plutus. */
  @Test
  void doesNotMisrepresentBlockedApprovalAsPlutusRejection() {
    latest("BLOCKED", false, result());
    var view = advice.read(cycle(7), null, "/plan");
    assertThat(view.status()).isEqualTo("UNAVAILABLE");
    assertThat(view.recommendation())
        .contains("aceite não foi concluído")
        .doesNotContain("pediu ajustes");
    assertThat(view.caution()).contains("recuperar o resultado");
    assertThat(view.figures()).isEmpty();
  }

  /** Ocorrência invalidada ou contrato alterado exige revisão mesmo sem estratégia posterior. */
  @Test
  void preservesInvalidatedOccurrenceAndVersionBoundary() {
    latest("COMPLETED", false, result());
    assertThat(advice.read(cycle(7), null, "/plan").status()).isEqualTo("STALE");
    latest("COMPLETED", true, result());
    var cycle = cycle(7);
    cycle.setVersionChangedAt(now.plusSeconds(60));
    assertThat(advice.read(cycle, null, "/plan").status()).isEqualTo("STALE");
  }

  /** Falha, cancelamento e processamento atuais não recuperam uma aprovação anterior. */
  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "IN_PROGRESS", "CANCELLED", "FAILED"})
  void neverApprovesUnfinishedAttempt(String status) {
    latest(status, false, result());
    var view = advice.read(cycle(7), null, "/plan");
    assertThat(view.status())
        .isEqualTo(List.of("PENDING", "IN_PROGRESS").contains(status) ? "PENDING" : "UNAVAILABLE");
    assertThat(view.figures()).isEmpty();
  }

  /** Ausência, números textuais e contradição financeira nunca viram zeros ou cálculo confiável. */
  @Test
  void rejectsMissingAndContradictoryNumbers() {
    assertThat(advice.read(cycle(7), null, "/plan").status()).isEqualTo("UNAVAILABLE");
    for (String payload :
        List.of(
            "{}",
            "null",
            "invalid json",
            result().replace("41.74", "\"41.74\""),
            result().replace("25.26", "50"))) {
      latest("COMPLETED", true, payload);
      assertThat(advice.read(cycle(7), null, "/plan").figures()).isEmpty();
    }
  }

  /** Plano vinculado prevalece e mantém contribuição por venda, distinta do total de receita. */
  @Test
  void prioritizesLinkedPlanAndKeepsLongReportBehindShortSummary() {
    var scenario =
        new PlanEvaluation.ScenarioResult(
            "BASE",
            "Base",
            10,
            0,
            new BigDecimal("670"),
            new BigDecimal("600"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.TEN,
            new BigDecimal("50"),
            new BigDecimal("30"),
            new BigDecimal("50"),
            new BigDecimal("32"),
            new BigDecimal("300"),
            new BigDecimal("200"),
            new BigDecimal("4"),
            BigDecimal.ZERO,
            true,
            List.of());
    String full = "Preço R$ 67, sobra R$ 30 após anúncios. ".repeat(40);
    var analysis =
        new PlanView.Analysis(
            988L,
            "COMPLETED",
            full,
            new ObjectMapper()
                .createObjectNode()
                .put("decision", "APPROVE")
                .put("executiveSummary", full),
            null,
            "model",
            null,
            "UNKNOWN",
            now);
    var plan =
        new PlanView(
            3L,
            "PRODUCT",
            7L,
            Environment.LIVE,
            "Plano",
            2,
            null,
            2L,
            1,
            "Operador",
            now,
            null,
            new PlanEvaluation("PROJECTED_VIABLE", "Projeção", List.of(), List.of(scenario)),
            false,
            List.of(),
            false,
            analysis);
    var view = advice.read(cycle(7), plan, "/plan");
    assertThat(view.status()).isEqualTo("APPROVE");
    assertThat(view.figures().getFirst().value()).isEqualByComparingTo("30");
    assertThat(view.recommendation()).hasSizeLessThanOrEqualTo(601);
    assertThat(view.fullRationale()).isEqualTo(full);
    verifyNoInteractions(tasks);
  }

  /**
   * Distingue análise em andamento, falha, ausência e vencimento sem anunciar espera inexistente.
   */
  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "RUNNING", "FAILED", "COMPLETED", "ABSENT", "STALE"})
  void distinguishesCommercialAnalysisStates(String state) {
    var analysis =
        "ABSENT".equals(state)
            ? null
            : new PlanView.Analysis(988L, state, null, null, null, "model", null, "UNKNOWN", now);
    var plan =
        new PlanView(
            3L,
            "PRODUCT",
            7L,
            Environment.LIVE,
            "Plano",
            2,
            null,
            2L,
            1,
            "Operador",
            now,
            null,
            null,
            "STALE".equals(state),
            List.of(),
            false,
            analysis);
    var view = advice.read(cycle(7), plan, "/plan");
    String expected =
        "STALE".equals(state)
            ? "STALE"
            : List.of("PENDING", "RUNNING").contains(state) ? "PENDING" : "UNAVAILABLE";
    assertThat(view.status()).isEqualTo(expected);
    if (!"PENDING".equals(expected)) assertThat(view.recommendation()).doesNotContain("aguarde");
    assertThat(view.figures()).isEmpty();
    verifyNoInteractions(tasks);
  }
}
