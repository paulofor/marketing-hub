package com.marketinghub.experiment.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorDecision;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployRequestDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeDeployStatus;
import com.marketinghub.experiment.monitoring.pde.PdeDeployStatusClient;
import com.marketinghub.experiment.monitoring.pde.PdeProductionDeploymentClient;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
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

    @Mock
    private PdeDeployStatusClient pdeDeployStatusClient;

    @Mock
    private PdeProductionDeploymentClient pdeProductionDeploymentClient;

    @Mock
    private PdeProductionSlotRepository pdeProductionSlotRepository;

    private PostDeployMonitorService service;

    /** Monta o serviço com dependências controladas para cenários comerciais. */
    @BeforeEach
    void setUp() {
        service = new PostDeployMonitorService(
                experimentRepository,
                campaignMetricRepository,
                apiLogService,
                pdeAnalyticsClient,
                pdeDeployStatusClient,
                pdeProductionDeploymentClient,
                pdeProductionSlotRepository);
        lenient().when(pdeDeployStatusClient.fetchStatuses()).thenReturn(List.of(availableDeployStatus()));
        lenient().when(pdeProductionDeploymentClient.isConfigured()).thenReturn(false);
        lenient().when(pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc("metodo-musa-7-dias"))
                .thenReturn(List.of(productionSlot("v1", "musa-pde-entry-v4-video-hero")));
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
                "2026-07-21T02:00:00Z",
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
                List.of(),
                List.of(),
                List.of(new PdeAnalyticsSummary.PdeSessionJourney(
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
        assertThat(response.pde().trafficSources())
                .extracting("utmContent")
                .contains("criativo-a");
        assertThat(response.pde().recentJourneys())
                .extracting("abandonmentPoint")
                .contains("SAIU_NA_PRIMEIRA_DOBRA");
        assertThat(response.pde().recentJourneys())
                .extracting("clientIp")
                .contains("203.0.113.10");
        assertThat(response.pdeDeployments())
                .extracting("environment")
                .contains("homolog");
        assertThat(response.pdeProductionSlots())
                .extracting("slotCode")
                .contains("v1");
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

    /** Alerta quando a homologação está publicada no backend, mas o frontend não responde. */
    @Test
    void alertsWhenPdeDeploymentFrontendIsNotReachable() {
        Experiment experiment = Experiment.builder().id(67L).build();
        when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric("8.00", null)));
        when(apiLogService.findLogs(67L, 50)).thenReturn(List.of());
        when(pdeDeployStatusClient.fetchStatuses()).thenReturn(List.of(new PdeDeployStatus(
                "homolog",
                true,
                "AVAILABLE",
                null,
                "docker-compose.homolog.yml",
                "abc123",
                "abc123",
                "musa-pde-entry-v4-video-hero",
                "http://191.252.102.54:5177",
                "http://191.252.102.54:8097",
                false,
                true,
                Instant.parse("2026-07-21T02:00:00Z"),
                List.of())));
        when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias")).thenReturn(emptyPdeSummary());

        var response = service.summarize(67L, null);

        assertThat(response.alerts()).anyMatch(alert -> alert.contains("frontend público sem resposta"));
        assertThat(response.pdeDeployments().get(0).composeFile()).isEqualTo("docker-compose.homolog.yml");
        assertThat(response.pdeDeployments().get(0).experienceVersion()).isEqualTo("musa-pde-entry-v4-video-hero");
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
                "2026-07-21T02:00:00Z",
                List.of(new PdeAnalyticsSummary.PdeEventMetric("PRESENCE_MAP_CHOICE_SELECTED", 6)),
                List.of(new PdeAnalyticsSummary.PdeExperienceVersionMetric(
                        "musa-pde-entry-v3", 120, 20, 20, 6, 0, 5, 2, 2, 1, 1)),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var response = service.summarize(67L, null);

        assertThat(response.decision()).isEqualTo(PostDeployMonitorDecision.SCALE_GRADUALLY);
        assertThat(response.pde().presenceMapClicks()).isEqualTo(6);
        assertThat(response.logs().totalLogs()).isEqualTo(1);
    }

    /** Libera o comando de produção quando homologação está saudável e produção está defasada. */
    @Test
    void exposesProductionDeployActionWhenHomologIsAhead() {
        Experiment experiment = Experiment.builder().id(67L).build();
        when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric("8.00", null)));
        when(apiLogService.findLogs(67L, 50)).thenReturn(List.of());
        when(pdeProductionDeploymentClient.isConfigured()).thenReturn(true);
        when(pdeDeployStatusClient.fetchStatuses()).thenReturn(List.of(
                deployStatus("homolog", "commit-homolog", true, true),
                deployStatus("production", "commit-production", true, true)));
        when(pdeAnalyticsClient.fetchSummary("metodo-musa-7-dias")).thenReturn(emptyPdeSummary());

        var response = service.summarize(67L, null);

        assertThat(response.pdePromotionControl().productionBehind()).isTrue();
        assertThat(response.pdePromotionControl().productionDeployAvailable()).isTrue();
        assertThat(response.alerts()).anyMatch(alert -> alert.contains("commits diferentes"));
    }

    /** Dispara o workflow produtivo quando o controle de promoção está liberado. */
    @Test
    void dispatchesProductionDeployWhenPromotionIsAvailable() {
        Experiment experiment = Experiment.builder().id(67L).build();
        when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
        when(pdeProductionDeploymentClient.isConfigured()).thenReturn(true);
        when(pdeDeployStatusClient.fetchStatuses()).thenReturn(List.of(
                deployStatus("homolog", "commit-homolog", true, true),
                deployStatus("production", "commit-production", true, true)));
        when(pdeProductionDeploymentClient.dispatchProductionDeploy(eq(67L), eq("Paulo"), eq("commit-homolog")))
                .thenReturn(new PostDeployPdeProductionDeployResponseDto(
                        true,
                        "DISPATCHED",
                        "ok",
                        "production",
                        "pde-platform-metodo-musa-ci.yml",
                        "commit-homolog",
                        Instant.parse("2026-07-21T02:10:00Z")));

        var response = service.requestProductionDeploy(
                67L,
                new PostDeployPdeProductionDeployRequestDto("Paulo", "commit-homolog"));

        assertThat(response.accepted()).isTrue();
        assertThat(response.status()).isEqualTo("DISPATCHED");
        verify(pdeProductionDeploymentClient).dispatchProductionDeploy(67L, "Paulo", "commit-homolog");
    }

    /** Bloqueia produção quando a homologação não está saudável. */
    @Test
    void blocksProductionDeployWhenHomologIsNotHealthy() {
        Experiment experiment = Experiment.builder().id(67L).build();
        when(experimentRepository.findById(67L)).thenReturn(Optional.of(experiment));
        when(pdeDeployStatusClient.fetchStatuses()).thenReturn(List.of(
                deployStatus("homolog", "commit-homolog", false, true),
                deployStatus("production", "commit-production", true, true)));

        var response = service.requestProductionDeploy(
                67L,
                new PostDeployPdeProductionDeployRequestDto("Paulo", "commit-homolog"));

        assertThat(response.accepted()).isFalse();
        assertThat(response.status()).isEqualTo("BLOCKED");
        assertThat(response.message()).contains("Valide homologação");
    }

    /** Cria slot produtivo com domínio normalizado para permitir URLs paralelas de hipótese PDE. */
    @Test
    void savesPdeProductionSlotForParallelHypothesisUrl() {
        Experiment experiment = Experiment.builder().id(71L).build();
        when(experimentRepository.findById(71L)).thenReturn(Optional.of(experiment));
        when(pdeProductionSlotRepository.findByProductSlugAndSlotCode("metodo-musa-7-dias", "v2"))
                .thenReturn(Optional.empty());
        when(pdeProductionSlotRepository.save(org.mockito.ArgumentMatchers.any(PdeProductionSlot.class)))
                .thenAnswer(invocation -> {
                    PdeProductionSlot slot = invocation.getArgument(0);
                    slot.setId(2L);
                    slot.setCreatedAt(Instant.parse("2026-07-24T10:00:00Z"));
                    slot.setUpdatedAt(Instant.parse("2026-07-24T10:00:00Z"));
                    return slot;
                });

        var response = service.saveProductionSlot(
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
                        "Hipotese 2"));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.domain()).isEqualTo("v2.clubemusa.com.br");
        assertThat(response.publicUrl()).isEqualTo("https://v2.clubemusa.com.br");
        assertThat(response.targetEnvironment()).isEqualTo("production-v2");
        assertThat(response.sourceExperimentId()).isEqualTo(71L);
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

    /** Cria um status de deploy ativo para não acionar alertas técnicos nos cenários base. */
    private PdeDeployStatus availableDeployStatus() {
        return new PdeDeployStatus(
                "homolog",
                true,
                "AVAILABLE",
                null,
                "docker-compose.homolog.yml",
                "abc123",
                "abc123",
                "musa-pde-entry-v4-video-hero",
                "http://191.252.102.54:5177",
                "http://191.252.102.54:8097",
                true,
                true,
                Instant.parse("2026-07-21T02:00:00Z"),
                List.of(new PdeDeployStatus.PdeDeployServiceStatus(
                        "pde-platform-frontend",
                        "pde-platform-frontend-homolog",
                        "ghcr.io/demo/pde-platform-frontend:abc123",
                        5177,
                        80,
                        "frontend")));
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

    /** Cria status de deploy parametrizado para comparar homologação e produção. */
    private PdeDeployStatus deployStatus(String environment, String commitSha, boolean frontendReachable, boolean backendReachable) {
        return new PdeDeployStatus(
                environment,
                backendReachable,
                backendReachable ? "AVAILABLE" : "UNAVAILABLE",
                null,
                "production".equals(environment) ? "docker-compose.deploy.yml" : "docker-compose.homolog.yml",
                commitSha,
                commitSha,
                "musa-pde-entry-v4-video-hero",
                "production".equals(environment) ? "https://clubemusa.com.br" : "http://191.252.102.54:5177",
                "production".equals(environment) ? "http://191.252.102.54:8096" : "http://191.252.102.54:8097",
                frontendReachable,
                backendReachable,
                Instant.parse("2026-07-21T02:00:00Z"),
                List.of());
    }

    /** Cria um resumo PDE sem conversão para cenários focados no status de deploy. */
    private PdeAnalyticsSummary emptyPdeSummary() {
        return new PdeAnalyticsSummary(
                "metodo-musa-7-dias",
                "musa-pde-entry-v4-video-hero",
                0,
                0,
                0,
                0,
                0,
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
