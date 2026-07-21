package com.marketinghub.experiment.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorDecision;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
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

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentCampaignMetricRepository campaignMetricRepository;

    @Mock
    private ExperimentFacebookApiLogService apiLogService;

    @Mock
    private PdeAnalyticsClient pdeAnalyticsClient;

    private PostDeployMonitorService service;

    /** Monta o serviço com dependências controladas para cenários comerciais. */
    @BeforeEach
    void setUp() {
        service = new PostDeployMonitorService(
                experimentRepository,
                campaignMetricRepository,
                apiLogService,
                pdeAnalyticsClient);
    }

    /** Recomenda pausa quando há gasto relevante sem primeira interação no PDE. */
    @Test
    void recommendsPauseWhenSpendReachesThresholdWithoutPdeInteraction() {
        Experiment experiment = Experiment.builder().id(67L).build();
        when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric("25.00", null)));
        when(apiLogService.findLogs(67L, 50)).thenReturn(List.of());
        when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias")).thenReturn(new PdeAnalyticsSummary(
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
                List.of(),
                List.of(new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v3", 80, 15, 15, 0, 0, 0, 0, 0, 0, 0)),
                List.of(new PdeAnalyticsSummary.PdeTrafficSourceMetric(
                        "facebook",
                        "musa-campanha",
                        "criativo-a",
                        15,
                        15,
                        0,
                        0,
                        0,
                        0,
                        0,
                        2000,
                        "2026-07-21T02:00:00Z")),
                List.of(new PdeAnalyticsSummary.PdeSessionJourney(
                        "session-1",
                        "visitor-1",
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
        assertThat(response.pde().experienceVersions())
                .extracting("experienceVersion")
                .contains("musa-pde-entry-v3");
        assertThat(response.pde().trafficSources())
                .extracting("utmContent")
                .contains("criativo-a");
        assertThat(response.pde().recentJourneys())
                .extracting("abandonmentPoint")
                .contains("SAIU_NA_PRIMEIRA_DOBRA");
    }

    /** Recomenda corrigir medição quando o PDE não responde ao Hub. */
    @Test
    void recommendsTechnicalAttentionWhenPdeAnalyticsFails() {
        Experiment experiment = Experiment.builder().id(67L).build();
        when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric("12.00", null)));
        when(apiLogService.findLogs(67L, 50)).thenReturn(List.of());
        when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias")).thenThrow(new IllegalStateException("offline"));

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
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric("8.00", null)));
        when(apiLogService.findLogs(67L, 50)).thenReturn(List.of(successLog()));
        when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias")).thenReturn(new PdeAnalyticsSummary(
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
                List.of(new PdeAnalyticsSummary.PdeEventMetric("PRESENCE_MAP_CHOICE_SELECTED", 6)),
                List.of(new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v3", 120, 20, 20, 6, 0, 5, 2, 2, 1, 1)),
                List.of(),
                List.of()));

        var response = service.summarize(67L, null);

        assertThat(response.decision()).isEqualTo(PostDeployMonitorDecision.SCALE_GRADUALLY);
        assertThat(response.pde().presenceMapClicks()).isEqualTo(6);
        assertThat(response.logs().totalLogs()).isEqualTo(1);
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
