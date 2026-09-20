package com.marketinghub.experiment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.directcontact.v1.ExperimentDirectContactService;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsSeverity;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsVisitorDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsVisitorsDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeCommercialOutcomeSummary;
import com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsReader;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa a consolidação comercial do cockpit de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentCockpitServiceTest {

  @Mock private ExperimentRepository experimentRepository;
  @Mock private ExperimentReadinessService readinessService;
  @Mock private ExperimentDiagnosticsService diagnosticsService;
  @Mock private ExperimentFunnelService funnelService;
  @Mock private ExperimentFunnelDiagnosticService funnelDiagnosticService;
  @Mock private ExperimentDirectContactService directContactService;
  @Mock private PdeExperimentAnalyticsReader pdeExperimentAnalyticsReader;

  @Spy
  private ExperimentSampleDecisionService sampleDecisionService =
      new ExperimentSampleDecisionService(new ExperimentBinomialConfidenceService());

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

  /** Garante que quatro visitantes do Vega não virem diagnóstico prematuro contra a página. */
  @Test
  void getCockpitUsesProgressiveSampleBeforeChangingSalesPage() {
    Long experimentId = 92L;
    Experiment experiment =
        Experiment.builder()
            .id(experimentId)
            .name("MUSA-H003-E003")
            .experimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL)
            .campaignObjective(ExperimentCampaignObjective.SALES)
            .platform(ExperimentPlatform.FACEBOOK)
            .sampleSize(100)
            .targetCvr(new BigDecimal("5.00"))
            .mediaSpendLimit(new BigDecimal("100.00"))
            .campaignMetric(
                ExperimentCampaignMetric.builder()
                    .impressions(120L)
                    .clicks(4L)
                    .spend(new BigDecimal("5.89"))
                    .cpc(new BigDecimal("1.47"))
                    .build())
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
    when(funnelService.summarize(experimentId))
        .thenReturn(
            List.of(
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 4),
                stage(ExperimentFunnelStage.ENVIO_FORM, 0),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)));
    when(funnelDiagnosticService.diagnose(experimentId))
        .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(), null));
    when(funnelService.approvedRevenue(experimentId)).thenReturn(BigDecimal.ZERO);
    PdeAnalyticsSummary pdeSummary = pdeSummary(4, 7);
    when(pdeExperimentAnalyticsReader.read(experiment)).thenReturn(pdeSummary);
    when(pdeExperimentAnalyticsReader.commercialOutcomes(experiment, pdeSummary))
        .thenReturn(pdeOutcomes(1, 1));

    var cockpit = service.getCockpit(experimentId);

    assertEquals(4, cockpit.scoreboard().humanVisitors());
    assertEquals("INSUFFICIENT_DATA", cockpit.sampleDecision().status());
    assertEquals(4, cockpit.sampleDecision().humanVisitors());
    assertEquals(0, cockpit.sampleDecision().purchases());
    assertEquals(100, cockpit.sampleDecision().initialTargetVisitors());
    assertEquals(5, cockpit.sampleDecision().targetPurchasesAtInitialDecision());
    assertEquals(500, cockpit.sampleDecision().precisionTargetVisitors());
    assertEquals("AMOSTRA_COMERCIAL_INSUFICIENTE", cockpit.bottleneck().code());
    assertEquals("PRESERVAR_TESTE", cockpit.nextActions().getFirst().code());
  }

  /** Bloqueia a leitura comercial quando a coorte PDE autoritativa não pode ser medida. */
  @Test
  void getCockpitBlocksCommercialReadingWhenPdeCohortIsUnavailable() {
    Long experimentId = 93L;
    Experiment experiment = preparePdeSalesExperiment(experimentId);
    when(pdeExperimentAnalyticsReader.read(experiment))
        .thenThrow(new IllegalStateException("PDE indisponível no teste"));

    var cockpit = service.getCockpit(experimentId);

    assertEquals("BLOCKED", cockpit.health().status());
    assertEquals("MEASUREMENT_UNAVAILABLE", cockpit.sampleDecision().status());
    assertEquals("MENSURACAO_AMOSTRA_INDISPONIVEL", cockpit.bottleneck().code());
    assertEquals("CORRIGIR_TRACKING", cockpit.nextActions().getFirst().code());
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

  /** Impede leitura comercial quando analytics, visitantes e funil divergem em pageviews. */
  @Test
  void getCockpitBlocksDivergentCanonicalPageViews() {
    Long experimentId = 90L;
    prepareRealExperimentWithAdMetrics(experimentId, 500L, 4L);
    var visitors =
        new ExperimentLandingAnalyticsVisitorsDto(
            2, 1, 1, List.of(visitor("visitante-1", 2), visitor("visitante-2", 1)));
    when(funnelService.summarizeLandingAnalytics(experimentId))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                7, 2, 2, 4, 13000, 6500, null, List.of(), List.of(), List.of(), null, visitors,
                List.of()));
    when(funnelService.summarize(experimentId))
        .thenReturn(List.of(stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 3)));

    var cockpit = service.getCockpit(experimentId);

    assertEquals("BLOCKED", cockpit.health().status());
    assertEquals("INTEGRIDADE_METRICAS_DIVERGENTE", cockpit.bottleneck().code());
    assertTrue(cockpit.health().blockers().getFirst().contains("Analytics registra 2"));
  }

  /** Libera a leitura quando analytics, visitantes e funil compartilham a contagem canônica. */
  @Test
  void getCockpitAcceptsAlignedCanonicalPageViews() {
    Long experimentId = 91L;
    prepareRealExperimentWithAdMetrics(experimentId, 500L, 4L);
    var visitors =
        new ExperimentLandingAnalyticsVisitorsDto(
            2, 1, 1, List.of(visitor("visitante-1", 2), visitor("visitante-2", 1)));
    when(funnelService.summarizeLandingAnalytics(experimentId))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                8, 2, 3, 4, 13000, 6500, null, List.of(), List.of(), List.of(), null, visitors,
                List.of()));
    when(funnelService.summarize(experimentId))
        .thenReturn(List.of(stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 3)));

    var cockpit = service.getCockpit(experimentId);

    assertEquals("READY", cockpit.health().status());
    assertEquals("PAGINA_SEM_CONVERSAO", cockpit.bottleneck().code());
  }

  /** Garante que custo legado e eventos pré-exposição não contaminam a inicialização comercial. */
  @Test
  void getCockpitStartsCommercialMetricsAtZeroUntilFirstVerifiedImpression() {
    ExperimentCampaignMetric campaignMetric =
        ExperimentCampaignMetric.builder()
            .impressions(0L)
            .clicks(0L)
            .spend(BigDecimal.ZERO)
            .cpc(BigDecimal.ZERO)
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(88L)
            .name("Agenda Cheia Nail Design")
            .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
            .campaignMetric(campaignMetric)
            .totalCost(new BigDecimal("0.14"))
            .build();
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    when(readinessService.summarize(88L))
        .thenReturn(
            new ExperimentReadinessSummaryDto(
                false, 0, false, 0, false, false, 0, 0, List.of(), List.of(), false, List.of()));
    when(diagnosticsService.diagnose(88L))
        .thenReturn(
            new ExperimentDiagnosticsDto(
                ExperimentDiagnosticsSeverity.INFO,
                "Pronto",
                "Sem bloqueio operacional.",
                null,
                List.of(),
                null));
    when(funnelService.summarizeLandingAnalytics(88L))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                60, 14, 13, 33, 1000, 70, null, List.of(), List.of(), List.of(), null, null,
                List.of()));
    when(funnelService.summarize(88L))
        .thenReturn(
            List.of(
                stage(ExperimentFunnelStage.VISUALIZACAO_ANUNCIO, 0),
                stage(ExperimentFunnelStage.ACESSO_FORM_LEAD, 0),
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 16),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)));
    when(funnelDiagnosticService.diagnose(88L))
        .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(), null));
    when(funnelService.approvedRevenue(88L)).thenReturn(BigDecimal.ZERO);

    var cockpit = service.getCockpit(88L);

    assertEquals(BigDecimal.ZERO, cockpit.scoreboard().spend());
    assertEquals(BigDecimal.ZERO, cockpit.scoreboard().margin());
    assertNull(cockpit.scoreboard().roas());
    assertNull(cockpit.scoreboard().ctr());
    assertNull(cockpit.scoreboard().cpc());
    assertEquals(0L, cockpit.scoreboard().pageViews());
    assertTrue(cockpit.funnel().stream().allMatch(stage -> stage.totalCount() == 0));
    assertEquals("AGUARDANDO_PRIMEIRA_IMPRESSAO", cockpit.bottleneck().code());
    assertEquals("AGUARDAR_PRIMEIRA_IMPRESSAO", cockpit.nextActions().get(0).code());
  }

  /** Mantém o piloto individual fora dos requisitos e das etapas exclusivas de campanha Meta. */
  @Test
  void getCockpitUsesDirectChannelWithoutDemandingMetaCampaign() {
    Long experimentId = 89L;
    Experiment experiment =
        Experiment.builder()
            .id(experimentId)
            .name("Rigel")
            .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
            .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
            .sampleSize(15)
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
                "Nenhuma inconsistência detectada",
                "Nenhuma campanha foi gerada ainda para este experimento.",
                null,
                List.of(),
                null));
    when(funnelService.summarizeLandingAnalytics(experimentId))
        .thenReturn(
            new ExperimentLandingAnalyticsDto(
                0, 0, 0, 0, 0, 0, null, List.of(), List.of(), List.of(), null, null, List.of()));
    when(funnelService.summarize(experimentId))
        .thenReturn(
            List.of(
                stage(ExperimentFunnelStage.VISUALIZACAO_ANUNCIO, 0),
                stage(ExperimentFunnelStage.ACESSO_FORM_LEAD, 0),
                stage(ExperimentFunnelStage.VISUALIZACAO_FORM, 0),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 0),
                stage(ExperimentFunnelStage.COMPRA, 0)));
    when(funnelDiagnosticService.diagnose(experimentId))
        .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(), null));
    when(funnelService.approvedRevenue(experimentId)).thenReturn(BigDecimal.ZERO);
    when(directContactService.countRecordedContacts(experimentId)).thenReturn(0L);
    when(directContactService.targetContacts(experiment)).thenReturn(15);

    var cockpit = service.getCockpit(experimentId);

    assertEquals("READY", cockpit.health().status());
    assertTrue(cockpit.health().description().contains("sem campanha Meta"));
    assertEquals("AGUARDANDO_CONTATOS_DIRETOS", cockpit.bottleneck().code());
    assertEquals(0L, cockpit.scoreboard().directContacts());
    assertEquals(15, cockpit.scoreboard().directContactTarget());
    assertTrue(cockpit.bottleneck().diagnosis().contains("0 de 15"));
    assertEquals("ACOMPANHAR_AMOSTRA_DIRETA", cockpit.nextActions().getFirst().code());
    assertTrue(
        cockpit.funnel().stream()
            .noneMatch(
                stage ->
                    stage.stage().equals("VISUALIZACAO_ANUNCIO")
                        || stage.stage().equals("ACESSO_FORM_LEAD")));
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

  /** Prepara um experimento PDE de vendas para validar bloqueios da medição de amostra. */
  private Experiment preparePdeSalesExperiment(Long experimentId) {
    Experiment experiment =
        Experiment.builder()
            .id(experimentId)
            .name("PDE com medição indisponível")
            .experimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL)
            .campaignObjective(ExperimentCampaignObjective.SALES)
            .platform(ExperimentPlatform.FACEBOOK)
            .sampleSize(100)
            .targetCvr(new BigDecimal("5.00"))
            .mediaSpendLimit(new BigDecimal("100.00"))
            .campaignMetric(
                ExperimentCampaignMetric.builder()
                    .impressions(120L)
                    .clicks(4L)
                    .spend(new BigDecimal("5.89"))
                    .build())
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
    return experiment;
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

  /** Cria um visitante provável mínimo com a quantidade informada de pageviews válidos. */
  private ExperimentLandingAnalyticsVisitorDto visitor(String visitorId, long pageViews) {
    return new ExperimentLandingAnalyticsVisitorDto(
        visitorId, 1, pageViews, null, null, 0, 1, null, "mobile", "Mobile", pageViews > 1);
  }

  /** Cria resumo PDE mínimo com visitantes distintos e sessões humanas atribuídos. */
  private PdeAnalyticsSummary pdeSummary(long humanVisitors, long humanSessions) {
    return new PdeAnalyticsSummary(
        "metodo-musa-7-dias",
        "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
        humanSessions,
        humanVisitors,
        humanSessions,
        humanSessions,
        humanSessions,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  /** Cria desfecho financeiro mínimo com compras e reembolsos deduplicados. */
  private PdeCommercialOutcomeSummary pdeOutcomes(long purchases, long refunds) {
    return new PdeCommercialOutcomeSummary(
        purchases,
        refunds,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        true,
        true,
        true,
        true,
        0,
        0,
        0,
        0,
        0);
  }
}
