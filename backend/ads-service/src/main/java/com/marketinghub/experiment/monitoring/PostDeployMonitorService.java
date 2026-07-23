package com.marketinghub.experiment.monitoring;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.monitoring.dto.PostDeployFacebookLogSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployMetaAdsSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorDecision;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeDeviceDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeDeployEnvironmentDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeDeployServiceDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeExperienceVersionDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployRequestDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdePromotionControlDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeScreenSizeDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeSessionJourneyDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeTrafficSourceDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeDeployStatus;
import com.marketinghub.experiment.monitoring.pde.PdeDeployStatusClient;
import com.marketinghub.experiment.monitoring.pde.PdeProductionDeploymentClient;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Consolida Meta Ads, analytics PDE e logs para decisão pós-deploy do experimento. */
@Service
@Slf4j
public class PostDeployMonitorService {

    private static final String DEFAULT_PDE_PRODUCT_SLUG = "metodo-musa-7-dias";
    private static final BigDecimal ZERO_INTERACTION_SPEND_THRESHOLD = BigDecimal.valueOf(25);

    private final ExperimentRepository experimentRepository;
    private final ExperimentCampaignMetricRepository campaignMetricRepository;
    private final ExperimentFacebookApiLogService apiLogService;
    private final PdeAnalyticsClient pdeAnalyticsClient;
    private final PdeDeployStatusClient pdeDeployStatusClient;
    private final PdeProductionDeploymentClient pdeProductionDeploymentClient;

    /** Inicializa o agregador com as fontes persistidas do Hub e o cliente do PDE. */
    public PostDeployMonitorService(
            ExperimentRepository experimentRepository,
            ExperimentCampaignMetricRepository campaignMetricRepository,
            ExperimentFacebookApiLogService apiLogService,
            PdeAnalyticsClient pdeAnalyticsClient,
            PdeDeployStatusClient pdeDeployStatusClient,
            PdeProductionDeploymentClient pdeProductionDeploymentClient) {
        this.experimentRepository = experimentRepository;
        this.campaignMetricRepository = campaignMetricRepository;
        this.apiLogService = apiLogService;
        this.pdeAnalyticsClient = pdeAnalyticsClient;
        this.pdeDeployStatusClient = pdeDeployStatusClient;
        this.pdeProductionDeploymentClient = pdeProductionDeploymentClient;
    }

    /** Monta o painel pós-deploy para o experimento e produto PDE informados. */
    public PostDeployMonitorResponseDto summarize(Long experimentId, String productSlug) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
        String resolvedProductSlug = resolveProductSlug(productSlug);
        ExperimentCampaignMetric metric = campaignMetricRepository.findByExperiment(experiment).orElse(null);
        PostDeployMetaAdsSummaryDto metaAds = toMetaAdsSummary(metric);
        PostDeployPdeSummaryDto pde = fetchPdeSummary(resolvedProductSlug);
        List<PostDeployPdeDeployEnvironmentDto> pdeDeployments = fetchPdeDeployments();
        PostDeployPdePromotionControlDto pdePromotionControl = buildPromotionControl(pdeDeployments);
        PostDeployFacebookLogSummaryDto logs = summarizeLogs(experimentId);
        List<String> alerts = buildAlerts(metaAds, pde, pdeDeployments, pdePromotionControl, logs);
        PostDeployMonitorDecision decision = decide(metaAds, pde, logs);
        return new PostDeployMonitorResponseDto(
                experimentId,
                resolvedProductSlug,
                Instant.now(),
                decision,
                decisionLabel(decision),
                recommendation(decision, pde),
                metaAds,
                pde,
                pdePromotionControl,
                pdeDeployments,
                logs,
                alerts);
    }

    /** Solicita produção apenas quando homologação está saudável e produção está defasada. */
    public PostDeployPdeProductionDeployResponseDto requestProductionDeploy(
            Long experimentId,
            PostDeployPdeProductionDeployRequestDto request) {
        experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
        List<PostDeployPdeDeployEnvironmentDto> pdeDeployments = fetchPdeDeployments();
        PostDeployPdePromotionControlDto control = buildPromotionControl(pdeDeployments);
        if (!control.productionDeployAvailable()) {
            return new PostDeployPdeProductionDeployResponseDto(
                    false,
                    "BLOCKED",
                    control.recommendation(),
                    control.targetEnvironment(),
                    control.workflowFile(),
                    control.sourceCommitSha(),
                    Instant.now());
        }
        String requestedBy = request != null && StringUtils.hasText(request.requestedBy())
                ? request.requestedBy().trim()
                : "marketing-hub";
        String requestedCommit = request != null && StringUtils.hasText(request.sourceCommitSha())
                ? request.sourceCommitSha().trim()
                : control.sourceCommitSha();
        if (StringUtils.hasText(control.sourceCommitSha())
                && StringUtils.hasText(requestedCommit)
                && !control.sourceCommitSha().equals(requestedCommit)) {
            return new PostDeployPdeProductionDeployResponseDto(
                    false,
                    "BLOCKED",
                    "A homologação mudou desde que a tela foi carregada. Atualize o painel antes de publicar produção.",
                    control.targetEnvironment(),
                    control.workflowFile(),
                    control.sourceCommitSha(),
                    Instant.now());
        }
        return pdeProductionDeploymentClient.dispatchProductionDeploy(experimentId, requestedBy, control.sourceCommitSha());
    }

    /** Resolve o produto PDE padrão quando a tela não informa um slug específico. */
    private String resolveProductSlug(String productSlug) {
        return StringUtils.hasText(productSlug) ? productSlug.trim() : DEFAULT_PDE_PRODUCT_SLUG;
    }

    /** Converte a métrica persistida da campanha em resumo do painel. */
    private PostDeployMetaAdsSummaryDto toMetaAdsSummary(ExperimentCampaignMetric metric) {
        if (metric == null) {
            return new PostDeployMetaAdsSummaryDto(null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return new PostDeployMetaAdsSummaryDto(
                metric.getDateStart(),
                metric.getDateStop(),
                metric.getReach(),
                metric.getImpressions(),
                metric.getClicks(),
                metric.getLeads(),
                metric.getSpend(),
                metric.getCpc(),
                metric.getCpl(),
                calculateCtrPercent(metric),
                metric.getCampaign() != null ? metric.getCampaign().getMetricsLastSyncedAt() : null,
                metric.getCampaign() != null ? metric.getCampaign().getMetricsLastError() : null);
    }

    /** Calcula CTR percentual com arredondamento estável para exibição administrativa. */
    private BigDecimal calculateCtrPercent(ExperimentCampaignMetric metric) {
        if (metric.getClicks() == null || metric.getImpressions() == null || metric.getImpressions() == 0) {
            return null;
        }
        return BigDecimal.valueOf(metric.getClicks())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(metric.getImpressions()), 2, RoundingMode.HALF_UP);
    }

    /** Consulta o PDE e retorna um resumo indisponível quando houver falha técnica. */
    private PostDeployPdeSummaryDto fetchPdeSummary(String productSlug) {
        try {
            PdeAnalyticsSummary summary = pdeAnalyticsClient.fetchSummary(productSlug);
            return toPdeSummary(summary);
        } catch (Exception ex) {
            log.error("Falha ao consultar analytics PDE no monitor pós-deploy; productSlug={}", productSlug, ex);
            return new PostDeployPdeSummaryDto(
                    false,
                    "UNAVAILABLE",
                    ex.getMessage(),
                    null,
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
                    0,
                    null,
                    Map.<String, Long>of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
        }
    }

    /** Consulta os ambientes PDE publicados para controlar versão, compose e portas por deploy. */
    private List<PostDeployPdeDeployEnvironmentDto> fetchPdeDeployments() {
        try {
            return pdeDeployStatusClient.fetchStatuses().stream()
                    .map(this::toPdeDeployEnvironmentDto)
                    .toList();
        } catch (Exception ex) {
            log.error("Falha ao consultar status de deploy dos ambientes PDE", ex);
            return List.of(new PostDeployPdeDeployEnvironmentDto(
                    "unknown",
                    false,
                    "UNAVAILABLE",
                    ex.getMessage(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    null,
                    List.of()));
        }
    }

    /** Converte o status técnico do deploy para o contrato administrativo do monitor. */
    private PostDeployPdeDeployEnvironmentDto toPdeDeployEnvironmentDto(PdeDeployStatus status) {
        return new PostDeployPdeDeployEnvironmentDto(
                status.environment(),
                status.available(),
                status.status(),
                status.errorMessage(),
                status.composeFile(),
                status.commitSha(),
                status.imageTag(),
                status.experienceVersion(),
                status.frontendUrl(),
                status.backendUrl(),
                status.frontendReachable(),
                status.backendReachable(),
                status.deployedAt(),
                toPdeDeployServiceDtos(status.services()));
    }

    /** Converte os containers declarados pelo deploy PDE para exibição no painel. */
    private List<PostDeployPdeDeployServiceDto> toPdeDeployServiceDtos(
            List<PdeDeployStatus.PdeDeployServiceStatus> services) {
        if (services == null) {
            return List.of();
        }
        return services.stream()
                .map(service -> new PostDeployPdeDeployServiceDto(
                        service.name(),
                        service.containerName(),
                        service.image(),
                        service.publicPort(),
                        service.targetPort(),
                        service.role()))
                .toList();
    }

    /** Calcula o estado de promoção homologação-produção a partir dos deploys reais. */
    private PostDeployPdePromotionControlDto buildPromotionControl(
            List<PostDeployPdeDeployEnvironmentDto> pdeDeployments) {
        PostDeployPdeDeployEnvironmentDto homolog = findDeployment(pdeDeployments, "homolog");
        PostDeployPdeDeployEnvironmentDto production = findDeployment(pdeDeployments, "production");
        boolean homologAvailable = isDeploymentHealthy(homolog);
        boolean productionAvailable = isDeploymentHealthy(production);
        String homologCommit = homolog != null ? homolog.commitSha() : null;
        String productionCommit = production != null ? production.commitSha() : null;
        boolean hasComparableCommits = isKnownCommit(homologCommit) && isKnownCommit(productionCommit);
        boolean productionBehind = homologAvailable
                && hasComparableCommits
                && !homologCommit.equals(productionCommit);
        boolean productionUpToDate = homologAvailable
                && productionAvailable
                && hasComparableCommits
                && homologCommit.equals(productionCommit);
        boolean githubConfigured = pdeProductionDeploymentClient.isConfigured();
        boolean deployAvailable = productionBehind && githubConfigured;
        boolean deployBlocked = !deployAvailable;
        String workflowFile = "pde-platform-metodo-musa-ci.yml";
        return new PostDeployPdePromotionControlDto(
                homologAvailable,
                productionAvailable,
                productionBehind,
                productionUpToDate,
                deployAvailable,
                deployBlocked,
                promotionStatusLabel(homologAvailable, productionBehind, productionUpToDate, githubConfigured),
                promotionRecommendation(homologAvailable, productionBehind, productionUpToDate, githubConfigured),
                homologCommit,
                productionCommit,
                "production",
                workflowFile);
    }

    /** Busca um deploy por nome de ambiente sem depender de caixa alta/baixa. */
    private PostDeployPdeDeployEnvironmentDto findDeployment(
            List<PostDeployPdeDeployEnvironmentDto> deployments,
            String environment) {
        if (deployments == null) {
            return null;
        }
        return deployments.stream()
                .filter(deployment -> environment.equalsIgnoreCase(deployment.environment()))
                .findFirst()
                .orElse(null);
    }

    /** Confirma que backend e frontend públicos do ambiente estão respondendo. */
    private boolean isDeploymentHealthy(PostDeployPdeDeployEnvironmentDto deployment) {
        return deployment != null
                && deployment.available()
                && deployment.backendReachable()
                && deployment.frontendReachable();
    }

    /** Verifica se o commit pode ser usado para comparar homologação e produção. */
    private boolean isKnownCommit(String commitSha) {
        return StringUtils.hasText(commitSha) && !"unknown".equalsIgnoreCase(commitSha);
    }

    /** Define o rótulo executivo do controle de promoção do PDE. */
    private String promotionStatusLabel(
            boolean homologAvailable,
            boolean productionBehind,
            boolean productionUpToDate,
            boolean githubConfigured) {
        if (!homologAvailable) {
            return "Homologação não validada";
        }
        if (productionUpToDate) {
            return "Produção atualizada";
        }
        if (productionBehind && githubConfigured) {
            return "Produção pronta para publicar";
        }
        if (productionBehind) {
            return "Produção pendente sem token";
        }
        return "Aguardando comparação";
    }

    /** Define a recomendação operacional para publicar ou bloquear produção. */
    private String promotionRecommendation(
            boolean homologAvailable,
            boolean productionBehind,
            boolean productionUpToDate,
            boolean githubConfigured) {
        if (!homologAvailable) {
            return "Valide homologação com backend e frontend online antes de liberar produção.";
        }
        if (productionUpToDate) {
            return "Produção já está no mesmo commit da homologação. Não há deploy pendente.";
        }
        if (productionBehind && githubConfigured) {
            return "Homologação está saudável e produção está defasada. Pode solicitar deploy produtivo pelo Marketing Hub.";
        }
        if (productionBehind) {
            return "Produção está defasada, mas o backend não possui token do GitHub Actions para acionar o workflow.";
        }
        return "Não há evidência suficiente para liberar produção. Atualize o painel e confirme os commits dos ambientes.";
    }

    /** Converte o contrato do PDE em métricas comerciais específicas do painel. */
    private PostDeployPdeSummaryDto toPdeSummary(PdeAnalyticsSummary summary) {
        Map<String, Long> events = new LinkedHashMap<>();
        if (summary.events() != null) {
            summary.events().forEach(event -> events.put(event.eventType(), event.total()));
        }
        return new PostDeployPdeSummaryDto(
                true,
                "AVAILABLE",
                null,
                summary.currentExperienceVersion(),
                summary.totalEvents(),
                summary.uniqueVisitors(),
                summary.sessions(),
                summary.pedEntries(),
                summary.pageViews(),
                eventTotal(events, "PRESENCE_MAP_CHOICE_SELECTED"),
                eventTotal(events, "DIAGNOSTIC_CHOICE_SELECTED"),
                eventTotal(events, "FIELD_FILLED"),
                summary.loginStarted(),
                summary.loginCompleted(),
                summary.paywallViewed(),
                summary.subscriptionClicked(),
                summary.checkoutStarted(),
                summary.subscriptionApproved(),
                summary.totalVisibleMs(),
                null,
                events,
                toExperienceVersionDtos(summary),
                toTrafficSourceDtos(summary),
                toDeviceDtos(summary),
                toScreenSizeDtos(summary),
                toSessionJourneyDtos(summary));
    }

    /** Converte distribuição por dispositivo do PDE para exibição no painel administrativo. */
    private List<PostDeployPdeDeviceDto> toDeviceDtos(PdeAnalyticsSummary summary) {
        if (summary.deviceBreakdown() == null) {
            return List.of();
        }
        return summary.deviceBreakdown().stream()
                .map(device -> new PostDeployPdeDeviceDto(
                        device.deviceType(),
                        device.label(),
                        device.sessions(),
                        device.percentage()))
                .toList();
    }

    /** Converte distribuição por tamanho de tela do PDE para exibição no painel administrativo. */
    private List<PostDeployPdeScreenSizeDto> toScreenSizeDtos(PdeAnalyticsSummary summary) {
        if (summary.screenSizeBreakdown() == null) {
            return List.of();
        }
        return summary.screenSizeBreakdown().stream()
                .map(screen -> new PostDeployPdeScreenSizeDto(
                        screen.screenSize(),
                        screen.label(),
                        screen.width(),
                        screen.height(),
                        screen.sessions(),
                        screen.percentage()))
                .toList();
    }

    /** Converte as métricas por versão do PDE para exibição no painel administrativo. */
    private List<PostDeployPdeExperienceVersionDto> toExperienceVersionDtos(PdeAnalyticsSummary summary) {
        if (summary.experienceVersions() == null) {
            return List.of();
        }
        return summary.experienceVersions().stream()
                .map(version -> new PostDeployPdeExperienceVersionDto(
                        version.experienceVersion(),
                        version.totalEvents(),
                        version.sessions(),
                        version.pdeEntries(),
                        version.presenceMapClicks() + version.diagnosticClicks(),
                        version.loginStarted(),
                        version.paywallViewed(),
                        version.subscriptionClicked() + version.checkoutStarted(),
                        version.subscriptionApproved()))
                .toList();
    }

    /** Converte métricas por UTM/criativo para exibição no painel administrativo. */
    private List<PostDeployPdeTrafficSourceDto> toTrafficSourceDtos(PdeAnalyticsSummary summary) {
        if (summary.trafficSources() == null) {
            return List.of();
        }
        return summary.trafficSources().stream()
                .map(source -> new PostDeployPdeTrafficSourceDto(
                        source.utmSource(),
                        source.utmCampaign(),
                        source.utmContent(),
                        source.sessions(),
                        source.pdeEntries(),
                        source.firstInteractionClicks(),
                        source.loginStarted(),
                        source.paywallViewed(),
                        source.checkoutStarted(),
                        source.subscriptionApproved(),
                        source.totalVisibleMs(),
                        source.lastEventAt()))
                .toList();
    }

    /** Converte jornadas recentes do PDE sem expor a lista completa de passos técnicos. */
    private List<PostDeployPdeSessionJourneyDto> toSessionJourneyDtos(PdeAnalyticsSummary summary) {
        if (summary.recentJourneys() == null) {
            return List.of();
        }
        return summary.recentJourneys().stream()
                .map(journey -> new PostDeployPdeSessionJourneyDto(
                        journey.sessionId(),
                        journey.visitorId(),
                        journey.firstEventAt(),
                        journey.lastEventAt(),
                        journey.totalVisibleMs(),
                        journey.maxScrollDepthPercent(),
                        journey.screenNames(),
                        journey.sectionIds(),
                        journey.fieldFocused(),
                        journey.fieldInputStarted(),
                        journey.fieldFilled(),
                        journey.ctaClicked(),
                        journey.loginStarted(),
                        journey.loginCompleted(),
                        journey.paywallViewed(),
                        journey.checkoutStarted(),
                        journey.subscriptionApproved(),
                        journey.abandonmentPoint(),
                        journey.lastEventType(),
                        journey.lastActionName()))
                .toList();
    }

    /** Busca a contagem do evento considerando aliases de caixa alta e baixa. */
    private long eventTotal(Map<String, Long> events, String eventType) {
        Long direct = events.get(eventType);
        if (direct != null) {
            return direct;
        }
        String normalized = eventType.toLowerCase(Locale.ROOT);
        return events.entrySet().stream()
                .filter(entry -> normalized.equals(entry.getKey().toLowerCase(Locale.ROOT)))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /** Resume os logs recentes da API Meta vinculados ao experimento. */
    private PostDeployFacebookLogSummaryDto summarizeLogs(Long experimentId) {
        List<ExperimentFacebookApiLogDto> logs = apiLogService.findLogs(experimentId, 50);
        int errorLogs = (int) logs.stream().filter(this::isErrorLog).count();
        List<String> recentErrors = logs.stream()
                .filter(this::isErrorLog)
                .map(this::formatLogError)
                .limit(5)
                .toList();
        Instant lastLogAt = logs.stream()
                .map(this::logInstant)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        return new PostDeployFacebookLogSummaryDto(logs.size(), errorLogs, lastLogAt, recentErrors);
    }

    /** Identifica falhas de integração por status HTTP ou mensagem de erro. */
    private boolean isErrorLog(ExperimentFacebookApiLogDto log) {
        return (log.statusCode() != null && log.statusCode() >= 400)
                || StringUtils.hasText(log.errorMessage());
    }

    /** Formata um erro recente sem expor payloads sensíveis. */
    private String formatLogError(ExperimentFacebookApiLogDto log) {
        String endpoint = StringUtils.hasText(log.endpoint()) ? log.endpoint() : "endpoint não informado";
        String status = log.statusCode() != null ? "HTTP " + log.statusCode() : "sem status";
        String message = StringUtils.hasText(log.errorMessage()) ? " - " + log.errorMessage() : "";
        return endpoint + " (" + status + ")" + message;
    }

    /** Resolve o horário operacional mais útil do log. */
    private Instant logInstant(ExperimentFacebookApiLogDto log) {
        if (log.requestedAt() != null) {
            return log.requestedAt();
        }
        if (log.respondedAt() != null) {
            return log.respondedAt();
        }
        return log.createdAt();
    }

    /** Monta alertas objetivos para separar falha técnica de leitura comercial. */
    private List<String> buildAlerts(PostDeployMetaAdsSummaryDto metaAds,
                                     PostDeployPdeSummaryDto pde,
                                     List<PostDeployPdeDeployEnvironmentDto> pdeDeployments,
                                     PostDeployPdePromotionControlDto promotionControl,
                                     PostDeployFacebookLogSummaryDto logs) {
        List<String> alerts = new ArrayList<>();
        if (!pde.available()) {
            alerts.add("Analytics PDE indisponível; validar saúde do pde-platform antes de concluir resultado comercial.");
        }
        pdeDeployments.stream()
                .filter(deployment -> !deployment.available() || !deployment.backendReachable() || !deployment.frontendReachable())
                .map(this::deployAlert)
                .forEach(alerts::add);
        if (StringUtils.hasText(metaAds.lastSyncError())) {
            alerts.add("Sincronização de métricas Meta Ads possui erro recente.");
        }
        if (logs.errorLogs() > 0) {
            alerts.add("Há erros recentes nos logs da API Meta Ads.");
        }
        if (promotionControl.productionBehind()) {
            alerts.add("Homologação e produção estão em commits diferentes; não liberar tráfego sem publicar ou decidir manter produção antiga.");
        }
        if (spendAtLeast(metaAds, ZERO_INTERACTION_SPEND_THRESHOLD) && pde.available() && firstInteractionCount(pde) == 0) {
            alerts.add("Gasto atingiu o limite de atenção sem clique no Mapa/Diagnóstico.");
        }
        return alerts;
    }

    /** Descreve o problema de deploy em linguagem operacional para decisão comercial. */
    private String deployAlert(PostDeployPdeDeployEnvironmentDto deployment) {
        if (!deployment.available()) {
            return "Deploy PDE " + deployment.environment() + " indisponível; o backend do ambiente não respondeu.";
        }
        if (!deployment.frontendReachable()) {
            return "Deploy PDE " + deployment.environment() + " com backend ativo, mas frontend público sem resposta.";
        }
        return "Deploy PDE " + deployment.environment() + " possui status incompleto.";
    }

    /** Decide a ação operacional recomendada com base em gasto, interação e saúde técnica. */
    private PostDeployMonitorDecision decide(PostDeployMetaAdsSummaryDto metaAds,
                                             PostDeployPdeSummaryDto pde,
                                             PostDeployFacebookLogSummaryDto logs) {
        if (!pde.available() || StringUtils.hasText(metaAds.lastSyncError()) || logs.errorLogs() > 0) {
            return PostDeployMonitorDecision.TECHNICAL_ATTENTION;
        }
        if (pde.subscriptionApproved() > 0) {
            return PostDeployMonitorDecision.SCALE_GRADUALLY;
        }
        if (spendAtLeast(metaAds, ZERO_INTERACTION_SPEND_THRESHOLD) && firstInteractionCount(pde) == 0) {
            return PostDeployMonitorDecision.PAUSE_AND_FIX;
        }
        if ((metaAds.spend() == null || BigDecimal.ZERO.compareTo(metaAds.spend()) == 0) && pde.sessions() == 0) {
            return PostDeployMonitorDecision.WAITING_DATA;
        }
        return PostDeployMonitorDecision.KEEP_MONITORING;
    }

    /** Verifica se o gasto rastreado atingiu o limite informado. */
    private boolean spendAtLeast(PostDeployMetaAdsSummaryDto metaAds, BigDecimal threshold) {
        return metaAds.spend() != null && metaAds.spend().compareTo(threshold) >= 0;
    }

    /** Conta a primeira interação comercial relevante dentro do PDE. */
    private long firstInteractionCount(PostDeployPdeSummaryDto pde) {
        return pde.presenceMapClicks() + pde.diagnosticClicks();
    }

    /** Retorna o rótulo em português da decisão sugerida. */
    private String decisionLabel(PostDeployMonitorDecision decision) {
        return switch (decision) {
            case WAITING_DATA -> "Aguardando dados";
            case KEEP_MONITORING -> "Manter monitoramento";
            case PAUSE_AND_FIX -> "Pausar e corrigir";
            case SCALE_GRADUALLY -> "Escalar gradualmente";
            case TECHNICAL_ATTENTION -> "Corrigir medição";
        };
    }

    /** Retorna a recomendação executiva para a próxima decisão comercial. */
    private String recommendation(PostDeployMonitorDecision decision, PostDeployPdeSummaryDto pde) {
        return switch (decision) {
            case WAITING_DATA -> "Ainda não há volume suficiente para leitura. Aguarde tráfego real pós-deploy.";
            case KEEP_MONITORING -> buildKeepMonitoringRecommendation(pde);
            case PAUSE_AND_FIX -> "Pausar a campanha e ajustar a primeira dobra/CTA do PDE antes de gastar mais mídia.";
            case SCALE_GRADUALLY -> "Compra aprovada detectada. Manter a versão e escalar orçamento em passos pequenos.";
            case TECHNICAL_ATTENTION -> "Corrigir integração, logs ou sincronização antes de tomar decisão comercial.";
        };
    }

    /** Detalha a recomendação quando ainda existe sinal intermediário no funil. */
    private String buildKeepMonitoringRecommendation(PostDeployPdeSummaryDto pde) {
        if (firstInteractionCount(pde) > 0 && pde.fieldFilled() == 0) {
            return "Há clique inicial no PDE, mas o e-mail ainda não avançou. Monitorar e preparar ajuste na captura.";
        }
        if (pde.paywallViewed() > 0 && pde.subscriptionApproved() == 0) {
            return "O funil chegou ao paywall, mas ainda não comprou. Monitorar oferta, preço e objeções finais.";
        }
        return "Continuar coletando dados até atingir volume mínimo ou sinal de checkout/compra.";
    }
}
