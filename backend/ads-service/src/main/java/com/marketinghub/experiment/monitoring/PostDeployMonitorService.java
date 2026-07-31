package com.marketinghub.experiment.monitoring;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.monitoring.dto.PostDeployFacebookLogSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployMetaAdsSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorDecision;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeBuildIdentityDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeDeviceDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeExperienceVersionDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeScreenSizeDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeSessionJourneyDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeTrafficSourceDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeTrafficQualityDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeBuildIdentity;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Consolida Meta Ads, analytics PDE e logs para decisão pós-deploy do experimento. */
@Service
@Slf4j
public class PostDeployMonitorService {

  private static final BigDecimal ZERO_INTERACTION_SPEND_THRESHOLD = BigDecimal.valueOf(25);

  private final ExperimentRepository experimentRepository;
  private final ExperimentCampaignMetricRepository campaignMetricRepository;
  private final ExperimentFacebookApiLogService apiLogService;
  private final PdeAnalyticsClient pdeAnalyticsClient;
  private final PdeProductionSlotService pdeProductionSlotService;
  private final JdbcTemplate jdbcTemplate;

  /** Inicializa o agregador com as fontes persistidas do Hub e o cliente do PDE. */
  public PostDeployMonitorService(
      ExperimentRepository experimentRepository,
      ExperimentCampaignMetricRepository campaignMetricRepository,
      ExperimentFacebookApiLogService apiLogService,
      PdeAnalyticsClient pdeAnalyticsClient,
      PdeProductionSlotService pdeProductionSlotService,
      JdbcTemplate jdbcTemplate) {
    this.experimentRepository = experimentRepository;
    this.campaignMetricRepository = campaignMetricRepository;
    this.apiLogService = apiLogService;
    this.pdeAnalyticsClient = pdeAnalyticsClient;
    this.pdeProductionSlotService = pdeProductionSlotService;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Monta o painel pós-deploy para o experimento e produto PDE informados. */
  public PostDeployMonitorResponseDto summarize(Long experimentId, String productSlug) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Experimento %d não encontrado".formatted(experimentId)));
    String resolvedProductSlug = pdeProductionSlotService.resolveProductSlug(productSlug);
    ExperimentCampaignMetric metric =
        campaignMetricRepository.findByExperiment(experiment).orElse(null);
    PostDeployMetaAdsSummaryDto metaAds = toMetaAdsSummary(metric);
    List<PostDeployPdeProductionSlotDto> pdeProductionSlots =
        pdeProductionSlotService.listProductionSlotsForProduct(resolvedProductSlug);
    PostDeployPdeProductionSlotDto monitoredSlot =
        resolveMonitoredSlot(experiment, pdeProductionSlots);
    String monitoredExperienceVersion =
        monitoredSlot != null ? monitoredSlot.experienceVersion() : null;
    String monitoredPublicUrl = monitoredSlot != null ? monitoredSlot.publicUrl() : null;
    boolean campaignTrafficActive = hasCampaignTraffic(metaAds);
    List<String> attributionCodes = fetchExperimentAttributionCodes(experimentId);
    PostDeployPdeSummaryDto pde =
        fetchPdeSummary(
            resolvedProductSlug,
            monitoredExperienceVersion,
            monitoredPublicUrl,
            campaignTrafficActive,
            attributionCodes);
    PostDeployPdeBuildIdentityDto pdeBuildIdentity = fetchPdeBuildIdentity(monitoredPublicUrl);
    PostDeployFacebookLogSummaryDto logs = summarizeLogs(experimentId);
    List<String> alerts = buildAlerts(metaAds, pde, logs);
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
        pdeBuildIdentity,
        pdeProductionSlots,
        logs,
        alerts);
  }

  /** Consulta a identidade da build PDE pela mesma URL pública usada pelo cockpit. */
  private PostDeployPdeBuildIdentityDto fetchPdeBuildIdentity(String monitoredPublicUrl) {
    try {
      PdeBuildIdentity identity = pdeAnalyticsClient.fetchBuildIdentity(monitoredPublicUrl);
      if (identity == null) {
        return unavailableBuildIdentity(monitoredPublicUrl, "EMPTY_RESPONSE", null);
      }
      return new PostDeployPdeBuildIdentityDto(
          true,
          "AVAILABLE",
          null,
          monitoredPublicUrl,
          identity.applicationName(),
          identity.artifact(),
          identity.buildVersion(),
          identity.commitSha(),
          identity.branch(),
          identity.imageTag(),
          identity.backendImage(),
          identity.environment(),
          identity.backendUrl(),
          identity.frontendUrl(),
          identity.marketingHubBaseUrl(),
          identity.deployedAt());
    } catch (Exception ex) {
      log.error(
          "Falha ao consultar identidade da build PDE; monitoredPublicUrl={}",
          monitoredPublicUrl,
          ex);
      return unavailableBuildIdentity(monitoredPublicUrl, ex.getClass().getSimpleName(), ex.getMessage());
    }
  }

  /** Monta resposta de identidade indisponível preservando a URL usada na consulta. */
  private PostDeployPdeBuildIdentityDto unavailableBuildIdentity(
      String monitoredPublicUrl, String status, String errorMessage) {
    return new PostDeployPdeBuildIdentityDto(
        false,
        status,
        errorMessage,
        monitoredPublicUrl,
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
        null);
  }

  /** Lista os slots produtivos do produto PDE após validar o experimento de origem. */
  public List<PostDeployPdeProductionSlotDto> listProductionSlots(
      Long experimentId, String productSlug) {
    experimentRepository
        .findById(experimentId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Experimento %d não encontrado".formatted(experimentId)));
    return pdeProductionSlotService.listProductionSlotsForProduct(productSlug);
  }

  /** Cria ou atualiza um slot produtivo versionado para manter hipóteses PDE em URLs paralelas. */
  public PostDeployPdeProductionSlotDto saveProductionSlot(
      Long experimentId, PostDeployPdeProductionSlotRequestDto request) {
    experimentRepository
        .findById(experimentId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Experimento %d não encontrado".formatted(experimentId)));
    return pdeProductionSlotService.saveProductionSlot(
        request.productSlug(), experimentId, request);
  }

  /** Valida a entrega pública de um slot PDE antes de usar a URL em campanha. */
  public PostDeployPdeProductionSlotDto validateProductionSlot(
      Long experimentId, String productSlug, String slotCode) {
    experimentRepository
        .findById(experimentId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Experimento %d não encontrado".formatted(experimentId)));
    return pdeProductionSlotService.validateProductionSlot(productSlug, slotCode);
  }

  /** Converte a métrica persistida da campanha em resumo do painel. */
  private PostDeployMetaAdsSummaryDto toMetaAdsSummary(ExperimentCampaignMetric metric) {
    if (metric == null) {
      return new PostDeployMetaAdsSummaryDto(
          null, null, null, null, null, null, null, null, null, null, null, null);
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
    if (metric.getClicks() == null
        || metric.getImpressions() == null
        || metric.getImpressions() == 0) {
      return null;
    }
    return BigDecimal.valueOf(metric.getClicks())
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(metric.getImpressions()), 2, RoundingMode.HALF_UP);
  }

  /** Consulta o PDE e retorna um resumo indisponível quando houver falha técnica. */
  private PostDeployPdeSummaryDto fetchPdeSummary(
      String productSlug,
      String monitoredExperienceVersion,
      String monitoredPublicUrl,
      boolean campaignTrafficActive,
      List<String> attributionCodes) {
    try {
      PdeAnalyticsSummary summary = pdeAnalyticsClient.fetchSummary(productSlug, monitoredPublicUrl);
      return toPdeSummary(summary, monitoredExperienceVersion, campaignTrafficActive, attributionCodes);
    } catch (Exception ex) {
      log.error(
          "Falha ao consultar analytics PDE no monitor pós-deploy; productSlug={}, monitoredPublicUrl={}",
          productSlug,
          monitoredPublicUrl,
          ex);
      return new PostDeployPdeSummaryDto(
          false,
          "UNAVAILABLE",
          ex.getMessage(),
          "UNAVAILABLE",
          "Analytics PDE indisponível",
          "Corrigir disponibilidade do PDE antes de avaliar campanha ou validação.",
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
          List.of(),
          List.of());
    }
  }

  /** Resolve o slot PDE que o experimento deve monitorar a partir do vínculo e da URL de destino. */
  private PostDeployPdeProductionSlotDto resolveMonitoredSlot(
      Experiment experiment, List<PostDeployPdeProductionSlotDto> pdeProductionSlots) {
    if (pdeProductionSlots == null) {
      return null;
    }
    Long experimentId = experiment.getId();
    return pdeProductionSlots.stream()
        .filter(slot -> experimentId.equals(slot.sourceExperimentId()))
        .findFirst()
        .orElseGet(() -> resolveMonitoredSlotByDestination(experiment, pdeProductionSlots));
  }

  /** Resolve o slot PDE pelo host da URL de destino quando o vínculo de origem ainda não existe. */
  private PostDeployPdeProductionSlotDto resolveMonitoredSlotByDestination(
      Experiment experiment, List<PostDeployPdeProductionSlotDto> pdeProductionSlots) {
    String destinationHost = normalizeHost(experiment.getFollowUpActionUrl());
    if (!StringUtils.hasText(destinationHost)) {
      return null;
    }
    return pdeProductionSlots.stream()
        .filter(slot -> destinationHost.equals(normalizeHost(slot.publicUrl())))
        .findFirst()
        .orElse(null);
  }

  /** Normaliza host de URL produtiva para comparar destino do anúncio e slot PDE. */
  private String normalizeHost(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      String host = URI.create(value.trim()).getHost();
      if (StringUtils.hasText(host)) {
        return host.toLowerCase(Locale.ROOT);
      }
    } catch (Exception ex) {
      log.warn("Falha ao normalizar host de URL PDE; value={}", value, ex);
    }
    return value.trim().replaceFirst("(?i)^https?://", "").split("/")[0].toLowerCase(Locale.ROOT);
  }

  /** Converte o contrato do PDE em métricas comerciais específicas do painel. */
  private PostDeployPdeSummaryDto toPdeSummary(
      PdeAnalyticsSummary summary,
      String monitoredExperienceVersion,
      boolean campaignTrafficActive,
      List<String> attributionCodes) {
    List<String> safeAttributionCodes = normalizeAttributionCodes(attributionCodes);
    if (!safeAttributionCodes.isEmpty() && summary.trafficSources() != null) {
      return toAttributedPdeSummary(
          summary, monitoredExperienceVersion, campaignTrafficActive, safeAttributionCodes);
    }
    Map<String, Long> events = new LinkedHashMap<>();
    if (summary.events() != null) {
      summary.events().forEach(event -> events.put(event.eventType(), event.total()));
    }
    String currentExperienceVersion =
        StringUtils.hasText(monitoredExperienceVersion)
            ? monitoredExperienceVersion
            : summary.currentExperienceVersion();
    return new PostDeployPdeSummaryDto(
        true,
        "AVAILABLE",
        null,
        campaignTrafficActive ? "CAMPAIGN_PERFORMANCE" : "PRE_LAUNCH_VALIDATION",
        campaignTrafficActive ? "Performance de campanha" : "Validação pré-campanha",
        campaignTrafficActive
            ? "Usar estes dados para leitura de funil enquanto houver entrega Meta sincronizada."
            : "Usar estes dados apenas para QA/acessos internos; não tomar decisão de campanha até Meta Ads registrar entrega.",
        currentExperienceVersion,
        summary.totalEvents(),
        summary.rawTotalEvents(),
        summary.uniqueVisitors(),
        summary.sessions(),
        summary.rawSessions(),
        summary.humanSessions(),
        summary.botSuspectedSessions(),
        summary.platformCrawlerSessions(),
        summary.internalQaSessions(),
        summary.unknownSessions(),
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
        calculateAverageVisibleMsPerSession(summary),
        parseInstant(summary.lastEventAt()),
        events,
        toExperienceVersionDtos(summary),
        toTrafficSourceDtos(summary),
        toTrafficQualityDtos(summary),
        toDeviceDtos(summary),
        toScreenSizeDtos(summary),
        toSessionJourneyDtos(summary));
  }

  /** Converte analytics PDE usando somente UTMs/códigos ligados ao experimento monitorado. */
  private PostDeployPdeSummaryDto toAttributedPdeSummary(
      PdeAnalyticsSummary summary,
      String monitoredExperienceVersion,
      boolean campaignTrafficActive,
      List<String> attributionCodes) {
    List<PdeAnalyticsSummary.PdeTrafficSourceMetric> matchingTrafficSources =
        summary.trafficSources().stream()
            .filter(source -> source != null && matchesPdeAttribution(source, attributionCodes))
            .toList();
    long sessions =
        matchingTrafficSources.stream().mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::sessions).sum();
    long pdeEntries =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::pdeEntries)
            .sum();
    long firstInteractionClicks =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::firstInteractionClicks)
            .sum();
    long videoPartial =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::videoPartial)
            .sum();
    long videoComplete =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::videoComplete)
            .sum();
    long loginStarted =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::loginStarted)
            .sum();
    long paywallViewed =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::paywallViewed)
            .sum();
    long checkoutStarted =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::checkoutStarted)
            .sum();
    long subscriptionApproved =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::subscriptionApproved)
            .sum();
    long totalVisibleMs =
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::totalVisibleMs)
            .sum();
    Map<String, Long> events = new LinkedHashMap<>();
    events.put("VIDEO_PARTIAL", videoPartial);
    events.put("VIDEO_COMPLETED", videoComplete);
    String currentExperienceVersion =
        StringUtils.hasText(monitoredExperienceVersion)
            ? monitoredExperienceVersion
            : summary.currentExperienceVersion();
    long totalEvents =
        pdeEntries
            + firstInteractionClicks
            + videoPartial
            + videoComplete
            + loginStarted
            + paywallViewed
            + checkoutStarted
            + subscriptionApproved;
    return new PostDeployPdeSummaryDto(
        true,
        "AVAILABLE",
        null,
        campaignTrafficActive ? "CAMPAIGN_PERFORMANCE" : "PRE_LAUNCH_VALIDATION",
        campaignTrafficActive
            ? "Performance atribuída ao experimento"
            : "Validação atribuída ao experimento",
        campaignTrafficActive
            ? "KPIs filtrados por campanha/criativo do experimento; histórico de outras campanhas fica fora da decisão."
            : "Eventos filtrados por campanha/criativo do experimento; aguarde entrega Meta para leitura comercial.",
        currentExperienceVersion,
        totalEvents,
        totalEvents,
        sessions,
        sessions,
        sessions,
        sessions,
        0,
        0,
        0,
        0,
        pdeEntries,
        0,
        firstInteractionClicks,
        0,
        0,
        loginStarted,
        0,
        paywallViewed,
        0,
        checkoutStarted,
        subscriptionApproved,
        totalVisibleMs,
        sessions > 0 ? totalVisibleMs / sessions : 0,
        lastAttributedEventAt(matchingTrafficSources),
        events,
        List.of(
            new PostDeployPdeExperienceVersionDto(
                currentExperienceVersion,
                totalEvents,
                sessions,
                pdeEntries,
                firstInteractionClicks,
                videoPartial,
                videoComplete,
                loginStarted,
                paywallViewed,
                checkoutStarted,
                subscriptionApproved)),
        toTrafficSourceDtosFromMetrics(matchingTrafficSources),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  /** Calcula o tempo médio visível por sessão PDE para leitura comercial do funil. */
  private long calculateAverageVisibleMsPerSession(PdeAnalyticsSummary summary) {
    return summary.sessions() > 0 ? summary.totalVisibleMs() / summary.sessions() : 0;
  }

  /** Converte distribuição por dispositivo do PDE para exibição no painel administrativo. */
  private List<PostDeployPdeDeviceDto> toDeviceDtos(PdeAnalyticsSummary summary) {
    if (summary.deviceBreakdown() == null) {
      return List.of();
    }
    return summary.deviceBreakdown().stream()
        .map(
            device ->
                new PostDeployPdeDeviceDto(
                    device.deviceType(), device.label(), device.sessions(), device.percentage()))
        .toList();
  }

  /** Converte distribuição por tamanho de tela do PDE para exibição no painel administrativo. */
  private List<PostDeployPdeScreenSizeDto> toScreenSizeDtos(PdeAnalyticsSummary summary) {
    if (summary.screenSizeBreakdown() == null) {
      return List.of();
    }
    return summary.screenSizeBreakdown().stream()
        .map(
            screen ->
                new PostDeployPdeScreenSizeDto(
                    screen.screenSize(),
                    screen.label(),
                    screen.width(),
                    screen.height(),
                    screen.sessions(),
                    screen.percentage()))
        .toList();
  }

  /** Converte as métricas por versão do PDE para exibição no painel administrativo. */
  private List<PostDeployPdeExperienceVersionDto> toExperienceVersionDtos(
      PdeAnalyticsSummary summary) {
    if (summary.experienceVersions() == null) {
      return List.of();
    }
    return summary.experienceVersions().stream()
        .map(
            version ->
                new PostDeployPdeExperienceVersionDto(
                    version.experienceVersion(),
                    version.totalEvents(),
                    version.sessions(),
                    version.pdeEntries(),
                    version.presenceMapClicks() + version.diagnosticClicks(),
                    version.videoPartial(),
                    version.videoComplete(),
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
    return toTrafficSourceDtosFromMetrics(summary.trafficSources());
  }

  /** Converte origens PDE já selecionadas para o contrato do painel. */
  private List<PostDeployPdeTrafficSourceDto> toTrafficSourceDtosFromMetrics(
      List<PdeAnalyticsSummary.PdeTrafficSourceMetric> trafficSources) {
    if (trafficSources == null) {
      return List.of();
    }
    return trafficSources.stream()
        .map(
            source ->
                new PostDeployPdeTrafficSourceDto(
                    source.trafficChannel(),
                    source.utmSource(),
                    source.utmMedium(),
                    source.utmCampaign(),
                    source.utmContent(),
                    source.sessions(),
                    source.pdeEntries(),
                    source.firstInteractionClicks(),
                    source.videoPartial(),
                    source.videoComplete(),
                    source.loginStarted(),
                    source.paywallViewed(),
                    source.checkoutStarted(),
                    source.subscriptionApproved(),
                    source.firstInteractionRate(),
                    source.paywallRate(),
                    source.checkoutRate(),
                    source.purchaseRate(),
                    source.totalVisibleMs(),
                    source.lastEventAt()))
        .toList();
  }

  /** Busca códigos oficiais de campanha, conjunto, anúncio e UTM para atribuir o PDE ao experimento. */
  private List<String> fetchExperimentAttributionCodes(Long experimentId) {
    List<String> codes =
        jdbcTemplate.queryForList(
            """
                SELECT DISTINCT code
                FROM (
                    SELECT fac.id AS code
                    FROM facebook_ads_campaign fac
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT fac.external_id AS code
                    FROM facebook_ads_campaign fac
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT fas.id AS code
                    FROM facebook_ads_campaign fac
                    JOIN facebook_ads_ad_set fas ON fas.campaign_id = fac.id
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT fas.external_id AS code
                    FROM facebook_ads_campaign fac
                    JOIN facebook_ads_ad_set fas ON fas.campaign_id = fac.id
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT faa.id AS code
                    FROM facebook_ads_campaign fac
                    JOIN facebook_ads_ad_set fas ON fas.campaign_id = fac.id
                    JOIN facebook_ads_ad faa ON faa.adset_id = fas.id
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT faa.external_id AS code
                    FROM facebook_ads_campaign fac
                    JOIN facebook_ads_ad_set fas ON fas.campaign_id = fac.id
                    JOIN facebook_ads_ad faa ON faa.adset_id = fas.id
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT utm.utm_campaign AS code
                    FROM facebook_ads_campaign fac
                    JOIN facebook_ads_ad_set fas ON fas.campaign_id = fac.id
                    JOIN facebook_ads_ad faa ON faa.adset_id = fas.id
                    JOIN facebook_ads_ad_tracking_utm utm ON utm.ad_id = faa.id
                    WHERE fac.experiment_id = ?
                    UNION ALL
                    SELECT utm.utm_content AS code
                    FROM facebook_ads_campaign fac
                    JOIN facebook_ads_ad_set fas ON fas.campaign_id = fac.id
                    JOIN facebook_ads_ad faa ON faa.adset_id = fas.id
                    JOIN facebook_ads_ad_tracking_utm utm ON utm.ad_id = faa.id
                    WHERE fac.experiment_id = ?
                ) codes
                WHERE code IS NOT NULL
                  AND TRIM(code) <> ''
                """,
            String.class,
            experimentId,
            experimentId,
            experimentId,
            experimentId,
            experimentId,
            experimentId,
            experimentId,
            experimentId);
    return normalizeAttributionCodes(codes);
  }

  /** Normaliza códigos de atribuição para comparação estável entre Meta Ads e PDE. */
  private List<String> normalizeAttributionCodes(List<String> attributionCodes) {
    if (attributionCodes == null) {
      return List.of();
    }
    return attributionCodes.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
  }

  /** Confirma se a origem PDE pertence ao experimento por campanha ou criativo Meta. */
  private boolean matchesPdeAttribution(
      PdeAnalyticsSummary.PdeTrafficSourceMetric source, List<String> attributionCodes) {
    return attributionCodes.contains(source.utmCampaign())
        || attributionCodes.contains(source.utmContent());
  }

  /** Retorna o último evento entre as origens PDE atribuídas ao experimento. */
  private Instant lastAttributedEventAt(
      List<PdeAnalyticsSummary.PdeTrafficSourceMetric> trafficSources) {
    return trafficSources.stream()
        .map(PdeAnalyticsSummary.PdeTrafficSourceMetric::lastEventAt)
        .filter(StringUtils::hasText)
        .map(this::parseInstant)
        .max(Instant::compareTo)
        .orElse(null);
  }

  /** Converte a auditoria de qualidade de tráfego PDE para exibição no painel administrativo. */
  private List<PostDeployPdeTrafficQualityDto> toTrafficQualityDtos(PdeAnalyticsSummary summary) {
    if (summary.trafficQualityBreakdown() == null) {
      return List.of();
    }
    return summary.trafficQualityBreakdown().stream()
        .map(
            quality ->
                new PostDeployPdeTrafficQualityDto(
                    quality.trafficQuality(),
                    quality.label(),
                    quality.sessions(),
                    quality.events(),
                    quality.percentage()))
        .toList();
  }

  /** Converte jornadas recentes do PDE sem expor a lista completa de passos técnicos. */
  private List<PostDeployPdeSessionJourneyDto> toSessionJourneyDtos(PdeAnalyticsSummary summary) {
    if (summary.recentJourneys() == null) {
      return List.of();
    }
    return summary.recentJourneys().stream()
        .map(
            journey ->
                new PostDeployPdeSessionJourneyDto(
                    journey.sessionId(),
                    journey.visitorId(),
                    journey.clientIp(),
                    journey.userAgent(),
                    journey.trafficQuality(),
                    journey.trafficQualityReason(),
                    journey.trafficProvider(),
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

  /** Converte um instante textual opcional do PDE para o contrato do painel. */
  private Instant parseInstant(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return Instant.parse(value);
  }

  /** Resume os logs recentes da API Meta vinculados ao experimento. */
  private PostDeployFacebookLogSummaryDto summarizeLogs(Long experimentId) {
    List<ExperimentFacebookApiLogDto> logs = apiLogService.findLogs(experimentId, 50);
    int errorLogs = (int) logs.stream().filter(this::isErrorLog).count();
    List<String> recentErrors =
        logs.stream().filter(this::isErrorLog).map(this::formatLogError).limit(5).toList();
    Instant lastLogAt =
        logs.stream()
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
    String endpoint =
        StringUtils.hasText(log.endpoint()) ? log.endpoint() : "endpoint não informado";
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
  private List<String> buildAlerts(
      PostDeployMetaAdsSummaryDto metaAds,
      PostDeployPdeSummaryDto pde,
      PostDeployFacebookLogSummaryDto logs) {
    List<String> alerts = new ArrayList<>();
    if (!pde.available()) {
      alerts.add(
          "Analytics PDE indisponível; validar saúde do pde-platform antes de concluir resultado comercial.");
    }
    if (StringUtils.hasText(metaAds.lastSyncError())) {
      alerts.add("Sincronização de métricas Meta Ads possui erro recente.");
    }
    if (logs.errorLogs() > 0) {
      alerts.add("Há erros recentes nos logs da API Meta Ads.");
    }
    if (!hasCampaignTraffic(metaAds) && pde.available() && pde.sessions() > 0) {
      alerts.add(
          "Eventos PDE detectados antes de impressões Meta; tratar como validação pré-campanha, não performance.");
    }
    if (pde.available()
        && (pde.botSuspectedSessions() + pde.platformCrawlerSessions() + pde.internalQaSessions())
            > 0) {
      alerts.add(
          "Sessões de robô, crawler ou QA foram excluídas dos KPIs comerciais e preservadas na auditoria.");
    }
    if (spendAtLeast(metaAds, ZERO_INTERACTION_SPEND_THRESHOLD)
        && pde.available()
        && firstInteractionCount(pde) == 0) {
      alerts.add("Gasto atingiu o limite de atenção sem clique no Mapa/Diagnóstico.");
    }
    return alerts;
  }

  /** Decide a ação operacional recomendada com base em gasto, interação e saúde técnica. */
  private PostDeployMonitorDecision decide(
      PostDeployMetaAdsSummaryDto metaAds,
      PostDeployPdeSummaryDto pde,
      PostDeployFacebookLogSummaryDto logs) {
    if (!pde.available() || StringUtils.hasText(metaAds.lastSyncError()) || logs.errorLogs() > 0) {
      return PostDeployMonitorDecision.TECHNICAL_ATTENTION;
    }
    if (!hasCampaignTraffic(metaAds)) {
      return PostDeployMonitorDecision.WAITING_DATA;
    }
    if (pde.subscriptionApproved() > 0) {
      return PostDeployMonitorDecision.SCALE_GRADUALLY;
    }
    if (spendAtLeast(metaAds, ZERO_INTERACTION_SPEND_THRESHOLD)
        && firstInteractionCount(pde) == 0) {
      return PostDeployMonitorDecision.PAUSE_AND_FIX;
    }
    if ((metaAds.spend() == null || BigDecimal.ZERO.compareTo(metaAds.spend()) == 0)
        && pde.sessions() == 0) {
      return PostDeployMonitorDecision.WAITING_DATA;
    }
    return PostDeployMonitorDecision.KEEP_MONITORING;
  }

  /** Verifica se o gasto rastreado atingiu o limite informado. */
  private boolean spendAtLeast(PostDeployMetaAdsSummaryDto metaAds, BigDecimal threshold) {
    return metaAds.spend() != null && metaAds.spend().compareTo(threshold) >= 0;
  }

  /** Identifica se já existe entrega real da campanha Meta para leitura comercial. */
  private boolean hasCampaignTraffic(PostDeployMetaAdsSummaryDto metaAds) {
    return positive(metaAds.impressions())
        || positive(metaAds.clicks())
        || positive(metaAds.leads())
        || (metaAds.spend() != null && metaAds.spend().compareTo(BigDecimal.ZERO) > 0);
  }

  /** Verifica se uma métrica numérica foi sincronizada com valor positivo. */
  private boolean positive(Long value) {
    return value != null && value > 0;
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
      case WAITING_DATA ->
          "Ainda não há volume suficiente para leitura. Aguarde tráfego real pós-deploy.";
      case KEEP_MONITORING -> buildKeepMonitoringRecommendation(pde);
      case PAUSE_AND_FIX ->
          "Pausar a campanha e ajustar a primeira dobra/CTA do PDE antes de gastar mais mídia.";
      case SCALE_GRADUALLY ->
          "Compra aprovada detectada. Manter a versão e escalar orçamento em passos pequenos.";
      case TECHNICAL_ATTENTION ->
          "Corrigir integração, logs ou sincronização antes de tomar decisão comercial.";
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
