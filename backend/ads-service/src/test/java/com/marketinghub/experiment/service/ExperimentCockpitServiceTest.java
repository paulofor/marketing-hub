package com.marketinghub.experiment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsSeverity;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa a consolidação comercial do cockpit de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentCockpitServiceTest {

  @Mock private ExperimentRepository experimentRepository;
  @Mock private ExperimentReadinessService readinessService;
  @Mock private ExperimentDiagnosticsService diagnosticsService;
  @Mock private ExperimentFunnelService funnelService;
  @Mock private ExperimentFunnelDiagnosticService funnelDiagnosticService;

  @InjectMocks private ExperimentCockpitService service;

  /** Garante que experimento fake usa métricas reais atribuídas ao teste em vez de massa fixa. */
  @Test
  void getCockpitReturnsMeasuredDataForFakeExperiment() {
    Experiment experiment =
        Experiment.builder()
            .id(88L)
            .name("Simulação PDE")
            .experimentType(ExperimentType.FAKE_EXPERIMENT)
            .build();
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    when(funnelService.summarizeLandingAnalytics(88L))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                7, 3, 7, 0, 0, 0, null, List.of(), List.of(), List.of(), null, null, List.of()));
    when(funnelService.summarize(88L))
        .thenReturn(
            List.of(
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 7),
                stage(ExperimentFunnelStage.VIDEO_VISTO_PARCIAL, 4),
                stage(ExperimentFunnelStage.VIDEO_VISTO_COMPLETO, 2),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 1),
                stage(ExperimentFunnelStage.COMPRA, 0)));

    var cockpit = service.getCockpit(88L);

    assertEquals("FAKE_EXPERIMENT", cockpit.experimentType());
    assertEquals("SIMULATED", cockpit.health().status());
    assertEquals("FAKE_PDE_VIDEO_METRICAS", cockpit.bottleneck().code());
    assertEquals(7L, cockpit.scoreboard().pageViews());
    assertEquals(4L, cockpit.scoreboard().partialVideoViews());
    assertEquals(2L, cockpit.scoreboard().completeVideoViews());
    assertEquals(1L, cockpit.scoreboard().checkoutAccesses());
    assertEquals(5, cockpit.funnel().size());
    verify(funnelService).summarizeLandingAnalytics(88L);
    verify(funnelService).summarize(88L);
    verifyNoInteractions(readinessService, diagnosticsService, funnelDiagnosticService);
  }

  /** Garante que consumo de vídeo sem próximo passo recebe diagnóstico comercial específico. */
  @Test
  void getCockpitDiagnosesVideoEngagementWithoutNextStep() {
    ExperimentCampaignMetric campaignMetric =
        ExperimentCampaignMetric.builder().impressions(100L).clicks(20L).build();
    Experiment experiment =
        Experiment.builder()
            .id(77L)
            .name("MUSA v6")
            .experimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL)
            .campaignMetric(campaignMetric)
            .build();
    when(experimentRepository.findById(77L)).thenReturn(Optional.of(experiment));
    when(readinessService.summarize(77L))
        .thenReturn(
            new ExperimentReadinessSummaryDto(
                false, 0, false, 0, false, false, 0, 0, List.of(), List.of(), false, List.of()));
    when(diagnosticsService.diagnose(77L))
        .thenReturn(
            new ExperimentDiagnosticsDto(
                ExperimentDiagnosticsSeverity.INFO,
                "Pronto",
                "Sem bloqueio operacional.",
                null,
                List.of(),
                null));
    when(funnelService.summarizeLandingAnalytics(77L))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                0, 0, 0, 0, 0, 0, null, List.of(), List.of(), List.of(), null, null, List.of()));
    when(funnelService.summarize(77L))
        .thenReturn(
            List.of(
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 29),
                stage(ExperimentFunnelStage.VIDEO_VISTO_PARCIAL, 12),
                stage(ExperimentFunnelStage.VIDEO_VISTO_COMPLETO, 6),
                stage(ExperimentFunnelStage.ENVIO_FORM, 0),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)));
    when(funnelDiagnosticService.diagnose(77L))
        .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(), null));
    when(funnelService.approvedRevenue(77L)).thenReturn(BigDecimal.ZERO);

    var cockpit = service.getCockpit(77L);

    assertEquals(29, cockpit.scoreboard().pageViews());
    assertEquals(12, cockpit.scoreboard().partialVideoViews());
    assertEquals(6, cockpit.scoreboard().completeVideoViews());
    assertEquals("VIDEO_SEM_PROXIMO_PASSO", cockpit.bottleneck().code());
    assertEquals("REFORCAR_CTA_POS_VIDEO", cockpit.nextActions().get(0).code());
  }

  /** Garante que poucas impressões sem clique não recomendam troca prematura de criativo. */
  @Test
  void getCockpitKeepsCurrentCreativeWhenAdSampleIsInsufficient() {
    prepareRealExperimentWithAdMetrics(85L, 12L, 0L);

    var cockpit = service.getCockpit(85L);

    assertEquals("AMOSTRA_INSUFICIENTE_ANUNCIO", cockpit.bottleneck().code());
    assertEquals("CONTINUAR_COLETA", cockpit.nextActions().get(0).code());
  }

  /** Garante que a recomendação prudente permanece até a impressão anterior ao piso definido. */
  @Test
  void getCockpitKeepsCurrentCreativeImmediatelyBeforeMinimumSample() {
    prepareRealExperimentWithAdMetrics(86L, 199L, 0L);

    var cockpit = service.getCockpit(86L);

    assertEquals("AMOSTRA_INSUFICIENTE_ANUNCIO", cockpit.bottleneck().code());
    assertEquals("CONTINUAR_COLETA", cockpit.nextActions().get(0).code());
  }

  /** Garante que 200 impressões sem clique liberam a revisão de criativo e público. */
  @Test
  void getCockpitRecommendsCreativeReviewAtMinimumSample() {
    prepareRealExperimentWithAdMetrics(87L, 200L, 0L);

    var cockpit = service.getCockpit(87L);

    assertEquals("ANUNCIO_SEM_CLIQUE", cockpit.bottleneck().code());
    assertEquals("TROCAR_CRIATIVO", cockpit.nextActions().get(0).code());
  }

  /** Prepara um experimento real saudável, sem eventos de funil e com métricas de anúncio. */
  private void prepareRealExperimentWithAdMetrics(
      Long experimentId, Long impressions, Long clicks) {
    Experiment experiment =
        Experiment.builder()
            .id(experimentId)
            .name("Teste de amostra do anúncio")
            .experimentType(ExperimentType.NICHE_TEST)
            .campaignMetric(
                ExperimentCampaignMetric.builder().impressions(impressions).clicks(clicks).build())
            .build();
    when(experimentRepository.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(readinessService.summarize(experimentId))
        .thenReturn(
            new ExperimentReadinessSummaryDto(
                false, 0, false, 0, false, false, 0, 0, List.of(), List.of(), false, List.of()));
    when(diagnosticsService.diagnose(experimentId))
        .thenReturn(
            new ExperimentDiagnosticsDto(
                ExperimentDiagnosticsSeverity.INFO,
                "Pronto",
                "Sem bloqueio operacional.",
                null,
                List.of(),
                null));
    when(funnelService.summarizeLandingAnalytics(experimentId))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                0, 0, 0, 0, 0, 0, null, List.of(), List.of(), List.of(), null, null, List.of()));
    when(funnelService.summarize(experimentId)).thenReturn(List.of());
    when(funnelDiagnosticService.diagnose(experimentId))
        .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(), null));
    when(funnelService.approvedRevenue(experimentId)).thenReturn(BigDecimal.ZERO);
  }

  /** Cria uma etapa mínima do funil para o teste do cockpit. */
  private ExperimentFunnelStageDto stage(ExperimentFunnelStage stage, long totalCount) {
    ExperimentFunnelStageDto dto = new ExperimentFunnelStageDto();
    dto.setStage(stage);
    dto.setLabel(stage.getLabel());
    dto.setOrder(stage.getOrder());
    dto.setTotalCount(totalCount);
    return dto;
  }
}
