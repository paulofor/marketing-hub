package com.marketinghub.experiment.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorDecision;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa a decisão pós-deploy cruzando Meta Ads, PDE e logs. */
@ExtendWith(MockitoExtension.class)
class PostDeployMonitorServiceTest {

  @Mock private ExperimentRepository experimentRepository;

  @Mock private ExperimentCampaignMetricRepository campaignMetricRepository;

  @Mock private ExperimentFacebookApiLogService apiLogService;

  @Mock private PdeAnalyticsClient pdeAnalyticsClient;

  @Mock private PdeProductionSlotService pdeProductionSlotService;

  private PostDeployMonitorService service;

  /** Monta o serviço com dependências controladas para cenários comerciais. */
  @BeforeEach
  void setUp() {
    service =
        new PostDeployMonitorService(
            experimentRepository,
            campaignMetricRepository,
            apiLogService,
            pdeAnalyticsClient,
            pdeProductionSlotService);
    lenient()
        .when(pdeProductionSlotService.resolveProductSlug(null))
        .thenReturn("metodo-musa-7-dias");
    lenient()
        .when(pdeProductionSlotService.resolveProductSlug("metodo-musa-7-dias"))
        .thenReturn("metodo-musa-7-dias");
    lenient()
        .when(pdeProductionSlotService.listProductionSlotsForProduct("metodo-musa-7-dias"))
        .thenReturn(List.of(productionSlotDto("v1", "musa-pde-entry-v4-video-hero")));
  }

  /** Recomenda pausa quando há gasto relevante sem primeira interação no PDE. */
  @Test
  void recommendsPauseWhenSpendReachesThresholdWithoutPdeInteraction() {
    Experiment experiment = Experiment.builder().id(67L).build();
    when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
    when(campaignMetricRepository.findByExperiment(experiment))
        .thenReturn(Optional.of(metric("25.00", null)));
    when(apiLogService.findLogs(67L, 50)).thenReturn(List.of());
    when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias"))
        .thenReturn(
            new PdeAnalyticsSummary(
                "metodo-musa-7-dias",
                "musa-pde-entry-v3",
                80,
                20,
                15,
                15,
                15,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                2000,
                "2026-07-21T02:00:00Z",
                List.of(),
                List.of(
                    new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v3", 80, 15, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
                List.of(
                    new PdeAnalyticsSummary.PdeTrafficSourceMetric(
                        "Meta",
                        "facebook",
                        "paid_social",
                        "musa-campanha",
                        "criativo-a",
                        15,
                        15,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        2000,
                        "2026-07-21T02:00:00Z")),
                List.of(),
                List.of(),
                List.of(
                    new PdeAnalyticsSummary.PdeSessionJourney(
                        "session-1",
                        "visitor-1",
                        "203.0.113.10",
                        "2026-07-21T01:59:00Z",
                        "2026-07-21T02:00:00Z",
                        2000,
                        42,
                        List.of("login_first_access"),
                        List.of("login_hero"),
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        "SAIU_NA_PRIMEIRA_DOBRA",
                        "PAGE_VIEW",
                        "page_loaded"))));

    var response = service.summarize(67L, null);

    assertThat(response.decision()).isEqualTo(PostDeployMonitorDecision.PAUSE_AND_FIX);
    assertThat(response.alerts()).anyMatch(alert -> alert.contains("Mapa/Diagnóstico"));
    assertThat(response.metaAds().ctrPercent()).isEqualByComparingTo("5.00");
    assertThat(response.pde().currentExperienceVersion()).isEqualTo("musa-pde-entry-v3");
    assertThat(response.pde().averageVisibleMsPerSession()).isEqualTo(133);
    assertThat(response.pde().lastEventAt()).isEqualTo(Instant.parse("2026-07-21T02:00:00Z"));
    assertThat(response.pde().experienceVersions())
        .extracting("experienceVersion")
        .contains("musa-pde-entry-v3");
    assertThat(response.pde().trafficSources()).extracting("utmContent").contains("criativo-a");
    assertThat(response.pde().trafficSources()).extracting("trafficChannel").contains("Meta");
    assertThat(response.pde().recentJourneys())
        .extracting("abandonmentPoint")
        .contains("SAIU_NA_PRIMEIRA_DOBRA");
    assertThat(response.pde().recentJourneys()).extracting("clientIp").contains("203.0.113.10");
    assertThat(response.pdeProductionSlots()).extracting("slotCode").contains("v1");
  }

  /** Recomenda corrigir medição quando o PDE não responde ao Hub. */
  @Test
  void recommendsTechnicalAttentionWhenPdeAnalyticsFails() {
    Experiment experiment = Experiment.builder().id(67L).build();
    when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
    when(campaignMetricRepository.findByExperiment(experiment))
        .thenReturn(Optional.of(metric("12.00", null)));
    when(apiLogService.findLogs(67L, 50)).thenReturn(List.of());
    when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias"))
        .thenThrow(new IllegalStateException("offline"));

    var response = service.summarize(67L, "metodo-musa-7-dias");

    assertThat(response.decision()).isEqualTo(PostDeployMonitorDecision.TECHNICAL_ATTENTION);
    assertThat(response.pde().available()).isFalse();
    assertThat(response.alerts()).anyMatch(alert -> alert.contains("Analytics PDE indisponível"));
  }

  /** Recomenda escala gradual quando há compra aprovada no PDE. */
  @Test
  void recommendsScaleWhenPdeHasApprovedPurchase() {
    Experiment experiment = Experiment.builder().id(67L).build();
    when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
    when(campaignMetricRepository.findByExperiment(experiment))
        .thenReturn(Optional.of(metric("8.00", null)));
    when(apiLogService.findLogs(67L, 50)).thenReturn(List.of(successLog()));
    when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias"))
        .thenReturn(
            new PdeAnalyticsSummary(
                "metodo-musa-7-dias",
                "musa-pde-entry-v3",
                120,
                30,
                20,
                20,
                20,
                5,
                3,
                2,
                2,
                1,
                1,
                1,
                1,
                9000,
                "2026-07-21T02:00:00Z",
                List.of(new PdeAnalyticsSummary.PdeEventMetric("PRESENCE_MAP_CHOICE_SELECTED", 6)),
                List.of(
                    new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v3", 120, 20, 20, 6, 0, 0, 0, 5, 2, 2, 1, 1)),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

    var response = service.summarize(67L, null);

    assertThat(response.decision()).isEqualTo(PostDeployMonitorDecision.SCALE_GRADUALLY);
    assertThat(response.pde().presenceMapClicks()).isEqualTo(6);
    assertThat(response.logs().totalLogs()).isEqualTo(1);
  }

  /** Usa a versão do slot ligado ao experimento mesmo quando o PDE global informa outra versão. */
  @Test
  void usesExperimentProductionSlotAsCurrentPdeVersion() {
    Experiment experiment = Experiment.builder().id(76L).build();
    when(experimentRepository.findById(76L)).thenReturn(Optional.of(experiment));
    when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.empty());
    when(apiLogService.findLogs(76L, 50)).thenReturn(List.of());
    when(pdeProductionSlotService.listProductionSlotsForProduct("metodo-musa-7-dias"))
        .thenReturn(
            List.of(
                productionSlotDto("v5", "musa-pde-entry-v5-video-explicativo", 74L),
                productionSlotDto("v6", "musa-pde-entry-v6-video-motivacional", 76L)));
    when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias"))
        .thenReturn(
            new PdeAnalyticsSummary(
                "metodo-musa-7-dias",
                "musa-pde-entry-v5-video-explicativo",
                300,
                80,
                70,
                70,
                70,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                9000,
                "2026-07-28T16:03:26Z",
                List.of(),
                List.of(
                    new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v5-video-explicativo",
                        120,
                        30,
                        30,
                        0,
                        4,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0),
                    new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v6-video-motivacional",
                        180,
                        40,
                        40,
                        0,
                        2,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0)),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

    var response = service.summarize(76L, "metodo-musa-7-dias");

    assertThat(response.pde().currentExperienceVersion())
        .isEqualTo("musa-pde-entry-v6-video-motivacional");
    assertThat(response.pdeProductionSlots()).extracting("sourceExperimentId").contains(76L);
  }

  /** Mantém eventos PDE pré-campanha fora da decisão comercial quando a Meta ainda não entregou. */
  @Test
  void treatsPdeSessionsAsPreLaunchValidationWhenMetaHasNoDelivery() {
    Experiment experiment = Experiment.builder().id(76L).build();
    when(experimentRepository.findById(76L)).thenReturn(Optional.of(experiment));
    when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.empty());
    when(apiLogService.findLogs(76L, 50)).thenReturn(List.of());
    when(pdeProductionSlotService.listProductionSlotsForProduct("metodo-musa-7-dias"))
        .thenReturn(List.of(productionSlotDto("v6", "musa-pde-entry-v6-video-motivacional", 76L)));
    when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias"))
        .thenReturn(
            new PdeAnalyticsSummary(
                "metodo-musa-7-dias",
                "musa-pde-entry-v6-video-motivacional",
                936,
                67,
                67,
                67,
                67,
                4,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                12000,
                "2026-07-28T16:03:26Z",
                List.of(new PdeAnalyticsSummary.PdeEventMetric("PRESENCE_MAP_CHOICE_SELECTED", 4)),
                List.of(
                    new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v6-video-motivacional",
                        936,
                        67,
                        67,
                        4,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0)),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

    var response = service.summarize(76L, "metodo-musa-7-dias");

    assertThat(response.decision()).isEqualTo(PostDeployMonitorDecision.WAITING_DATA);
    assertThat(response.pde().measurementMode()).isEqualTo("PRE_LAUNCH_VALIDATION");
    assertThat(response.pde().measurementLabel()).isEqualTo("Validação pré-campanha");
    assertThat(response.alerts()).anyMatch(alert -> alert.contains("antes de impressões Meta"));
    assertThat(response.recommendation()).contains("tráfego real");
  }

  /** Cria slot produtivo com domínio normalizado para permitir URLs paralelas de hipótese PDE. */
  @Test
  void savesPdeProductionSlotForParallelHypothesisUrl() {
    Experiment experiment = Experiment.builder().id(71L).build();
    when(experimentRepository.findById(71L)).thenReturn(Optional.of(experiment));
    var savedSlot = productionSlotDto("v2", "musa-pde-entry-v5-estrada-desejo");
    when(pdeProductionSlotService.saveProductionSlot(
            eq("metodo-musa-7-dias"),
            eq(71L),
            org.mockito.ArgumentMatchers.any(PostDeployPdeProductionSlotRequestDto.class)))
        .thenReturn(savedSlot);

    var response =
        service.saveProductionSlot(
            71L,
            new PostDeployPdeProductionSlotRequestDto(
                "v2",
                "metodo-musa-7-dias",
                "https://v2.clubemusa.com.br/",
                null,
                null,
                "musa-pde-entry-v5-estrada-desejo",
                null,
                PdeProductionSlotStatus.PLANNED,
                null,
                "Hipotese 2",
                null,
                null));

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.domain()).isEqualTo("v2.clubemusa.com.br");
    assertThat(response.publicUrl()).isEqualTo("https://v2.clubemusa.com.br");
    assertThat(response.targetEnvironment()).isEqualTo("production-v2");
    assertThat(response.sourceExperimentId()).isEqualTo(70L);
  }

  /** Cria uma métrica Meta Ads mínima para os cenários do painel. */
  private ExperimentCampaignMetric metric(String spend, String lastError) {
    Experiment experiment = Experiment.builder().id(67L).build();
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId("campaign-67");
    campaign.setMetricsLastSyncedAt(Instant.parse("2026-07-21T02:00:00Z"));
    campaign.setMetricsLastError(lastError);
    return ExperimentCampaignMetric.builder()
        .experiment(experiment)
        .campaign(campaign)
        .dateStart(LocalDate.parse("2026-07-21"))
        .dateStop(LocalDate.parse("2026-07-21"))
        .impressions(100L)
        .clicks(5L)
        .spend(new BigDecimal(spend))
        .cpc(new BigDecimal("0.10"))
        .build();
  }

  /** Cria um slot produtivo persistido para validar a resposta do painel. */
  private PdeProductionSlot productionSlot(String slotCode, String experienceVersion) {
    return PdeProductionSlot.builder()
        .id(1L)
        .slotCode(slotCode)
        .productSlug("metodo-musa-7-dias")
        .domain(slotCode + ".clubemusa.com.br")
        .publicUrl("https://" + slotCode + ".clubemusa.com.br")
        .experienceVersion(experienceVersion)
        .targetEnvironment("production-" + slotCode)
        .status(PdeProductionSlotStatus.READY)
        .sourceExperimentId(70L)
        .createdAt(Instant.parse("2026-07-24T10:00:00Z"))
        .updatedAt(Instant.parse("2026-07-24T10:00:00Z"))
        .build();
  }

  /** Cria um DTO de slot produtivo para validar a resposta do painel. */
  private com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto
      productionSlotDto(String slotCode, String experienceVersion) {
    return productionSlotDto(slotCode, experienceVersion, 70L);
  }

  /** Cria um DTO de slot produtivo com experimento de origem específico. */
  private com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto
      productionSlotDto(String slotCode, String experienceVersion, Long sourceExperimentId) {
    PdeProductionSlot slot = productionSlot(slotCode, experienceVersion);
    return new com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto(
        slot.getId(),
        slot.getSlotCode(),
        slot.getProductSlug(),
        slot.getDomain(),
        slot.getPublicUrl(),
        slot.getBackendUrl(),
        slot.getExperienceVersion(),
        slot.getTargetEnvironment(),
        slot.getStatus(),
        sourceExperimentId,
        slot.getNotes(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        slot.getCreatedAt(),
        slot.getUpdatedAt());
  }

  /** Cria um log de integração sem falha para validar o resumo de logs. */
  private ExperimentFacebookApiLogDto successLog() {
    return new ExperimentFacebookApiLogDto(
        1L,
        null,
        null,
        null,
        null,
        null,
        null,
        "METRICS",
        "META",
        "/insights",
        "GET",
        200,
        null,
        Instant.parse("2026-07-21T02:00:00Z"),
        Instant.parse("2026-07-21T02:00:01Z"),
        1000L,
        null,
        null,
        Instant.parse("2026-07-21T02:00:01Z"));
  }
}
