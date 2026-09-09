package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsReader;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsSnapshot;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeCommercialOutcomeSummary;
import com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsReader;
import com.marketinghub.experiment.service.ExperimentCostReconciliationService;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida a fotografia automática sem acessar tráfego, pagamentos ou campanhas reais. */
class LearningCycleMeasurementCollectorTest {
  private static final Instant NOW = Instant.parse("2026-09-09T01:30:00Z");
  private final PdeExperimentAnalyticsReader analytics = mock(PdeExperimentAnalyticsReader.class);
  private final ExperimentAcquisitionMetricsReader acquisition =
      mock(ExperimentAcquisitionMetricsReader.class);
  private final ExperimentCostReconciliationService costs =
      mock(ExperimentCostReconciliationService.class);
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private LearningCycleMeasurementCollector collector;
  private Experiment experiment;
  private LearningSalesCycle cycle;

  /** Monta o recorte histórico do Vega com fontes determinísticas. */
  @BeforeEach
  void setUp() {
    collector = new LearningCycleMeasurementCollector(analytics, acquisition, costs, json);
    experiment =
        Experiment.builder()
            .id(91L)
            .product(Product.builder().id(4L).slug("metodo-musa-7-dias").build())
            .platform(ExperimentPlatform.FACEBOOK)
            .status(ExperimentStatus.USER_STOPPED)
            .build();
    cycle = new LearningSalesCycle();
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setExperimentId(91L);
    cycle.setProductVersion("musa-pde-entry-v7-espelho-antes-de-sair");
    cycle.setWindowStart(Instant.parse("2026-09-07T03:00:00Z"));
    cycle.setWindowEnd(Instant.parse("2026-09-12T02:59:00Z"));
    when(analytics.read(eq(experiment), any(Instant.class), any(Instant.class)))
        .thenReturn(summary(0));
    when(acquisition.read(experiment))
        .thenReturn(
            new ExperimentAcquisitionMetricsSnapshot(
                ExperimentPlatform.FACEBOOK,
                new BigDecimal("27.25"),
                Instant.parse("2026-09-07T22:24:14Z"),
                Instant.parse("2026-09-07T22:24:14Z"),
                null,
                120L,
                5L,
                true));
    when(analytics.commercialOutcomes(
            eq(experiment), any(), any(Instant.class), any(Instant.class)))
        .thenReturn(
            new PdeCommercialOutcomeSummary(
                0,
                0,
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                true,
                true,
                true,
                true,
                0,
                0,
                0,
                0,
                0));
    when(costs.enrich(eq(experiment), any(ExperimentDto.class)))
        .thenAnswer(
            call -> {
              ExperimentDto dto = call.getArgument(1);
              dto.setAuditableTotalCost(new BigDecimal("27.30"));
              dto.setLegacyTotalCost(new BigDecimal("27.30"));
              dto.setUnreconciledLegacyCost(BigDecimal.ZERO);
              return dto;
            });
  }

  /** Reproduz os números conhecidos do #91 e calcula contribuição sem digitação humana. */
  @Test
  void reconcilesVegaFromOfficialSources() {
    var result = collector.collect(cycle, experiment, NOW);

    assertThat(result.ready()).isTrue();
    assertThat(result.evidence().path("automatic").asBoolean()).isTrue();
    assertThat(result.evidence().path("sessions").asLong()).isEqualTo(4);
    assertThat(result.evidence().path("starts").asLong()).isEqualTo(2);
    assertThat(result.evidence().path("firstResults").asLong()).isEqualTo(1);
    assertThat(result.evidence().path("netSales").asLong()).isZero();
    assertThat(result.evidence().path("spendBrl").decimalValue()).isEqualByComparingTo("27.25");
    assertThat(result.evidence().path("revenueBrl").decimalValue()).isEqualByComparingTo("0.00");
    assertThat(result.evidence().path("contributionBrl").decimalValue())
        .isEqualByComparingTo("-27.30");
    assertThat(result.evidence().at("/sources/pdeAnalytics/humanEvents").asLong()).isEqualTo(86);
    assertThat(result.evidence().path("sourceFingerprint").asText()).hasSize(64);
  }

  /** Bloqueia aprovação legada sem compra canônica em vez de registrá-la como zero vendas. */
  @Test
  void blocksLegacyPaymentWithoutCanonicalPurchase() {
    when(analytics.read(eq(experiment), any(Instant.class), any(Instant.class)))
        .thenReturn(summary(1));

    var result = collector.collect(cycle, experiment, NOW);

    assertThat(result.ready()).isFalse();
    assertThat(result.evidence().path("dataValid").asBoolean()).isFalse();
    assertThat(result.evidence().path("blocker").asText())
        .contains("aprovação de assinatura sem o evento canônico de compra");
    assertThat(result.evidence().has("netSales")).isFalse();
  }

  /** Fonte indisponível permanece bloqueio explícito e nunca se transforma em contagem vazia. */
  @Test
  void blocksUnavailableAnalyticsWithoutInventingZeros() {
    when(analytics.read(eq(experiment), any(Instant.class), any(Instant.class)))
        .thenThrow(new IllegalStateException("PDE indisponível"));

    var result = collector.collect(cycle, experiment, NOW);

    assertThat(result.ready()).isFalse();
    assertThat(result.evidence().path("blocker").asText()).contains("fonte oficial");
    assertThat(result.evidence().has("sessions")).isFalse();
    assertThat(result.evidence().has("revenueBrl")).isFalse();
  }

  /** Reembolso sem a compra correlata bloqueia decisão mesmo quando os totais numéricos empatam. */
  @Test
  void blocksRefundWithoutMatchingPurchaseReference() {
    when(analytics.read(eq(experiment), any(Instant.class), any(Instant.class)))
        .thenReturn(summaryWithEvents(0, "PURCHASE_COMPLETED", "REFUND_CONFIRMED"));
    when(analytics.commercialOutcomes(
            eq(experiment), any(), any(Instant.class), any(Instant.class)))
        .thenReturn(
            new PdeCommercialOutcomeSummary(
                1,
                1,
                new BigDecimal("67.00"),
                new BigDecimal("67.00"),
                BigDecimal.ZERO.setScale(2),
                true,
                true,
                false,
                true,
                0,
                0,
                0,
                0,
                0));

    var result = collector.collect(cycle, experiment, NOW);

    assertThat(result.ready()).isFalse();
    assertThat(result.evidence().path("blocker").asText()).contains("reembolso sem compra");
  }

  /** Snapshot parcial da campanha não é convertido silenciosamente em gasto ou alcance zero. */
  @Test
  void blocksIncompleteCampaignSnapshot() {
    when(acquisition.read(experiment))
        .thenReturn(
            new ExperimentAcquisitionMetricsSnapshot(
                ExperimentPlatform.FACEBOOK, null, NOW, NOW, null, 120L, 5L, true));

    var result = collector.collect(cycle, experiment, NOW);

    assertThat(result.ready()).isFalse();
    assertThat(result.evidence().path("blocker").asText()).contains("snapshot da campanha");
    assertThat(result.evidence().has("spendBrl")).isFalse();
  }

  /** Tráfego técnico novo permanece auditável, mas não finge uma mudança comercial humana. */
  @Test
  void ignoresRawTestTrafficInTheCommercialFingerprint() {
    var first = collector.collect(cycle, experiment, NOW);
    when(analytics.read(eq(experiment), any(Instant.class), any(Instant.class)))
        .thenReturn(summaryWithRawEvents(0, 99, "TASTING_STARTED", "VALUE_MOMENT"));

    var afterQa = collector.collect(cycle, experiment, NOW);

    assertThat(afterQa.evidence().at("/sources/pdeAnalytics/rawEvents").asLong()).isEqualTo(99);
    assertThat(afterQa.evidence().path("sourceFingerprint").asText())
        .isEqualTo(first.evidence().path("sourceFingerprint").asText());
  }

  /** Constrói o resumo atribuído com quatro sessões e os 86 eventos históricos conhecidos. */
  private PdeAnalyticsSummary summary(long subscriptionApproved) {
    return summaryWithEvents(subscriptionApproved, "TASTING_STARTED", "VALUE_MOMENT");
  }

  /** Permite variar eventos agregados sem alterar identidade, sessões e segregação do cenário. */
  private PdeAnalyticsSummary summaryWithEvents(long subscriptionApproved, String... eventTypes) {
    return summaryWithRawEvents(subscriptionApproved, 86, eventTypes);
  }

  /** Varia apenas o volume bruto para comprovar que tráfego técnico não governa decisões. */
  private PdeAnalyticsSummary summaryWithRawEvents(
      long subscriptionApproved, long rawEvents, String... eventTypes) {
    return new PdeAnalyticsSummary(
        "metodo-musa-7-dias",
        "musa-pde-entry-v7-espelho-antes-de-sair",
        86,
        rawEvents,
        4,
        4,
        4,
        4,
        0,
        0,
        0,
        0,
        4,
        4,
        1,
        0,
        1,
        0,
        subscriptionApproved,
        0,
        0,
        0,
        120000,
        "2026-09-07T19:18:32Z",
        java.util.Arrays.stream(eventTypes)
            .map(
                type ->
                    new PdeAnalyticsSummary.PdeEventMetric(
                        type, "TASTING_STARTED".equals(type) ? 2 : 1))
            .toList(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }
}
