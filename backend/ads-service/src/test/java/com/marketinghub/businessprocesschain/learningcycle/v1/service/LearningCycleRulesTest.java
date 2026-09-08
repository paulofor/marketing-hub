package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand.Action;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir escala artificial, decisões prematuras e ciclos sem limite. */
class LearningCycleRulesTest {
  private static final Instant NOW = Instant.parse("2026-09-08T15:00:00Z");
  private final ObjectMapper json = new ObjectMapper();

  /** Monta a autorização restrita do experimento usado nos testes. */
  private LearningSalesCycle cycle() {
    var cycle = new LearningSalesCycle();
    cycle.setExperimentId(91L);
    cycle.setBudgetLimitBrl(new BigDecimal("100"));
    cycle.setWindowStart(NOW.minusSeconds(86400));
    cycle.setWindowEnd(NOW.plusSeconds(86400));
    return cycle;
  }

  /** Produz uma fotografia comercial segregada e rastreável. */
  private ObjectNode metrics() {
    var data = json.createObjectNode();
    data.put("experimentId", 91);
    data.put("currency", "BRL");
    data.put("source", "Conciliação local");
    data.put("periodStart", NOW.minusSeconds(86400).toString());
    data.put("periodEnd", NOW.minusSeconds(5).toString());
    data.put("observedAt", NOW.toString());
    for (String key :
        new String[] {"sessions", "starts", "firstResults", "checkouts", "netSales", "refunds"})
      data.put(key, 10);
    data.put("spendBrl", 40);
    data.put("revenueBrl", 200);
    data.put("contributionBrl", 100);
    data.put("dataValid", true);
    data.put("testDataExcluded", true);
    data.put("deliveryVerified", true);
    data.put("useVerified", true);
    data.put("satisfactionVerified", true);
    return data;
  }

  /** Mantém a progressão inicial e separa decisões do encerramento de atividades. */
  @Test
  void progressesOnlyAllowedStages() {
    assertEquals("PLANNING", LearningCycleRules.next("LEARNING"));
    assertThrows(ResponseStatusException.class, () -> LearningCycleRules.next("DECISION"));
    assertFalse(LearningCycleRules.actions("LEARNING").contains(Action.SCALE));
    assertTrue(LearningCycleRules.actions("MEASUREMENT").contains(Action.FIX_MEASUREMENT));
  }

  /** Aceita resultados válidos sem transformar amostra em venda. */
  @Test
  void validatesStructuredSnapshot() {
    assertDoesNotThrow(() -> LearningCycleRules.validateMetrics(cycle(), metrics(), NOW));
  }

  /** Orçamento zero permite coleta orgânica, mas não disfarça gasto de mídia não autorizado. */
  @Test
  void zeroBudgetNeverAuthorizesPositiveMediaSpend() {
    var cycle = cycle();
    cycle.setBudgetLimitBrl(BigDecimal.ZERO);
    var metrics = metrics();
    metrics.put("spendBrl", 0);
    assertNull(LearningCycleRules.collectionBlocker(cycle, metrics, NOW));
    metrics.put("spendBrl", 1);
    assertNotNull(LearningCycleRules.collectionBlocker(cycle, metrics, NOW));
  }

  /** Impede mistura entre experimentos, moeda, datas e ausência de qualidade declarada. */
  @ParameterizedTest
  @MethodSource("invalidFields")
  void rejectsInvalidMetric(String field) {
    var data = metrics();
    data.remove(field);
    assertThrows(
        ResponseStatusException.class,
        () -> LearningCycleRules.validateMetrics(cycle(), data, NOW));
  }

  /** Lista dimensões indispensáveis para interpretar a fotografia. */
  static Stream<String> invalidFields() {
    return Stream.of(
        "experimentId",
        "currency",
        "source",
        "periodStart",
        "periodEnd",
        "observedAt",
        "sessions",
        "netSales",
        "spendBrl",
        "contributionBrl",
        "dataValid",
        "testDataExcluded");
  }

  /** Recusa fotografia do experimento anterior e receita ou contribuição impossíveis. */
  @Test
  void rejectsCrossExperimentAndNegativeRevenue() {
    var data = metrics();
    data.put("experimentId", 90);
    assertThrows(
        ResponseStatusException.class,
        () -> LearningCycleRules.validateMetrics(cycle(), data, NOW));
    data.put("experimentId", 91);
    data.put("revenueBrl", -1);
    assertThrows(
        ResponseStatusException.class,
        () -> LearningCycleRules.validateMetrics(cycle(), data, NOW));
  }

  /** Exige nova decisão quando o orçamento ou o prazo já acabaram. */
  @Test
  void stopsCollectionAtBudgetAndTimeLimits() {
    var cycle = cycle();
    var data = metrics();
    data.put("spendBrl", 100);
    assertNotNull(LearningCycleRules.collectionBlocker(cycle, data, NOW));
    data.put("spendBrl", 99);
    assertNull(LearningCycleRules.collectionBlocker(cycle, data, NOW));
    assertNotNull(LearningCycleRules.collectionBlocker(cycle, data, cycle.getWindowEnd()));
  }

  /** Separa continuação de escala para permitir decisão sobre teste já encerrado. */
  @Test
  void scaleCanBeRequestedAfterWindowWithFreshReconciliation() {
    var cycle = cycle();
    cycle.setWindowEnd(NOW);
    var data = metrics();
    data.put("spendBrl", 100);
    var brief = json.createObjectNode().put("sampleTarget", 10).put("minimumNetSales", 5);
    assertNull(LearningCycleRules.scaleBlocker(cycle, data, brief, NOW));
    assertNotNull(LearningCycleRules.collectionBlocker(cycle, data, NOW));
  }

  /** Não confunde aprovação sintética, receita bruta ou entrega ausente com valor comprovado. */
  @ParameterizedTest
  @MethodSource("scaleFields")
  void blocksUnprovenScale(String field) {
    var data = metrics();
    data.put(field, false);
    assertNotNull(
        LearningCycleRules.scaleBlocker(
            cycle(),
            data,
            json.createObjectNode().put("sampleTarget", 10).put("minimumNetSales", 5),
            NOW));
  }

  /** Lista as comprovações comerciais necessárias à escala. */
  static Stream<String> scaleFields() {
    return Stream.of(
        "dataValid", "testDataExcluded", "deliveryVerified", "useVerified", "satisfactionVerified");
  }

  /** Bloqueia amostra insuficiente, margem negativa e uma leitura antiga apresentada como atual. */
  @Test
  void rejectsThinSamplesNegativeContributionAndStaleMetrics() {
    var data = metrics();
    var brief = json.createObjectNode().put("sampleTarget", 100).put("minimumNetSales", 5);
    assertNotNull(LearningCycleRules.scaleBlocker(cycle(), data, brief, NOW));
    brief.put("sampleTarget", 10);
    data.put("contributionBrl", -1);
    assertNotNull(LearningCycleRules.scaleBlocker(cycle(), data, brief, NOW));
    data.put("contributionBrl", 100);
    data.put("observedAt", NOW.minusSeconds(86401).toString());
    assertNotNull(LearningCycleRules.scaleBlocker(cycle(), data, brief, NOW));
  }
}
