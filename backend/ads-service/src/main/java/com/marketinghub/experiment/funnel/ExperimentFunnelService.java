package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.ExperimentPdeCockpitDiagnosticsDto;
import com.marketinghub.experiment.funnel.dto.PdeExperienceVersionDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.RegisterExperimentFunnelEventRequest;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDetailedEventDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDeviceDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsEvidenceDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsLoadMetricDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsOperatingSystemDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsScreenSizeDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSectionDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSessionDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsTrafficQualityDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsVisitorDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsVisitorsDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionEngagementContractV1;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import com.marketinghub.leadportal.dto.RegisterLeadPortalSubmissionRequest;
import com.marketinghub.model.Lead;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository.StageAggregation;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serviço que consolida as etapas do funil de vendas de um experimento. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentFunnelService {
  private static final String INTERNAL_TEST_CAMPAIGN_CODE = "__mh_internal_test__";

  private final ExperimentRepository experimentRepository;
  private final ExperimentFunnelEventRepository eventRepository;
  private final ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;
  private final LeadRepository leadRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ExperimentFunnelStandbyService standbyService;
  private final PdeAnalyticsClient pdeAnalyticsClient;
  private final InternalAnalyticsTrafficFilter internalAnalyticsTrafficFilter;
  private final PdeProductionSlotRepository pdeProductionSlotRepository;

  static final String FLOW_SCOPE_CONDITION =
      """
            (
                f.experiment_id = ?
                OR EXISTS (
                    SELECT 1
                    FROM experiment e
                    WHERE e.id = ?
                      AND e.lead_portal_flow_id = f.id
                )
            )
            """;
  private static final int MAX_CAMPAIGN_CODE_LENGTH = 190;
  private static final long PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS = 3;
  private static final String DEFAULT_PDE_PRODUCT_SLUG = "metodo-musa-7-dias";
  private static final Pattern MUSA_VERSIONED_HOST_PATTERN =
      Pattern.compile(
          "https?://v(\\d+)\\.clubemusa\\.com\\.br(?:[:/].*)?", Pattern.CASE_INSENSITIVE);

  /** Consolida as métricas automáticas e eventos registrados por etapa do funil. */
  public List<ExperimentFunnelStageDto> summarize(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages = bootstrapStages();
    Instant baseline = resolveBaseline(experiment);

    applyManualEvents(experiment.getId(), baseline, stages);
    applyAutomaticMetrics(experiment.getId(), baseline, stages);
    applyPdeMembershipMetrics(experiment, stages);
    adaptStagesForExperimentType(experiment, stages);
    fillDefaultSourceWhenMissing(stages);

    return stages.values().stream()
        .filter(stage -> shouldExposeStage(experiment, stage.getStage()))
        .sorted(Comparator.comparingInt(ExperimentFunnelStageDto::getOrder))
        .toList();
  }

  /** Diagnostica a integração PDE usada pelo cockpit para explicar filtros, versão e fallback. */
  public ExperimentPdeCockpitDiagnosticsDto diagnosePdeCockpitIntegration(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    String followUpActionUrl = experiment.getFollowUpActionUrl();
    Optional<String> normalizedDomain = normalizeDomainFromUrl(followUpActionUrl);
    Optional<String> versionToken = resolveVersionTokenFromUrl(followUpActionUrl);
    ExpectedPdeExperienceVersion expectedVersion =
        resolveExpectedPdeExperienceVersionDiagnostic(followUpActionUrl);
    if (!isPdeMembershipSubscriptionFunnel(experiment)) {
      return new ExperimentPdeCockpitDiagnosticsDto(
          experimentId,
          false,
          followUpActionUrl,
          normalizedDomain.orElse(null),
          followUpActionUrl,
          DEFAULT_PDE_PRODUCT_SLUG,
          false,
          null,
          expectedVersion.value().orElse(null),
          expectedVersion.source(),
          versionToken.orElse(null),
          null,
          false,
          List.of(),
          0,
          0,
          false,
          "NOT_PDE_MEMBERSHIP_FUNNEL",
          List.of(),
          null,
          null);
    }

    PdeAnalyticsSummary summary;
    try {
      summary = fetchPdeSummaryForExperiment(experiment);
    } catch (Exception ex) {
      log.error(
          "Falha ao diagnosticar analytics PDE do cockpit; experimentId={} productSlug={} followUpActionUrl={}",
          experimentId,
          DEFAULT_PDE_PRODUCT_SLUG,
          followUpActionUrl,
          ex);
      return new ExperimentPdeCockpitDiagnosticsDto(
          experimentId,
          true,
          followUpActionUrl,
          normalizedDomain.orElse(null),
          followUpActionUrl,
          DEFAULT_PDE_PRODUCT_SLUG,
          false,
          null,
          expectedVersion.value().orElse(null),
          expectedVersion.source(),
          versionToken.orElse(null),
          null,
          false,
          List.of(),
          0,
          0,
          false,
          "PDE_SUMMARY_ERROR",
          List.of(),
          ex.getClass().getSimpleName(),
          ex.getMessage());
    }
    List<String> attributionCodes = fetchExperimentAttributionCodes(experiment);
    return buildPdeCockpitDiagnostics(
        experimentId,
        followUpActionUrl,
        normalizedDomain.orElse(null),
        versionToken.orElse(null),
        expectedVersion,
        summary,
        attributionCodes);
  }

  /** Soma a receita aprovada atribuida ao experimento dentro do escopo canônico do funil. */
  public BigDecimal approvedRevenue(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    Instant baseline = resolveBaseline(experiment);
    BigDecimal revenue =
        jdbcTemplate.queryForObject(
            """
                SELECT COALESCE(SUM(p.amount), 0)
                FROM lead_portal_purchase p
                JOIN flow_submissions s ON s.id = p.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE %s
                  AND (p.payment_approved_at IS NOT NULL OR p.mp_status = 'approved')
                  AND (? IS NULL OR COALESCE(p.payment_approved_at, p.updated_at) > ?)
                """
                .formatted(FLOW_SCOPE_CONDITION),
            BigDecimal.class,
            experimentId,
            experimentId,
            baseline,
            baseline);
    return revenue != null ? revenue : BigDecimal.ZERO;
  }

  /** Consolida sessões e eventos de analytics da landing publicados no experimento. */
  public ExperimentLandingAnalyticsDto summarizeLandingAnalytics(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    boolean fakeExperiment = isFakeExperiment(experiment);
    Instant baseline = resolveBaseline(experiment);
    List<LandingAnalyticsEventRow> rows = fetchLandingAnalyticsEvents(experimentId, baseline);
    Map<String, LandingAnalyticsSessionAccumulator> sessions = new LinkedHashMap<>();
    for (LandingAnalyticsEventRow row : rows) {
      Map<String, String> payload = parseDelimitedPayload(row.payload());
      String sessionId = firstNonBlank(payload.get("sessionId"), "sem-sessao");
      String eventType = firstNonBlank(payload.get("eventType"), "desconhecido");
      String sectionId = payload.get("sectionId");
      String pageUrl = payload.get("pageUrl");
      String userAgent = payload.get("userAgent");
      String deviceType = payload.get("deviceType");
      String operatingSystem = payload.get("operatingSystem");
      Integer screenWidth = parsePositiveInteger(payload.get("screenWidth"));
      Integer screenHeight = parsePositiveInteger(payload.get("screenHeight"));
      long elapsedMs = parseLong(payload.get("elapsedMs"));
      TrafficDiagnosis traffic =
          classifyLandingTraffic(payload, payload.get("visitorId"), fakeExperiment);

      LandingAnalyticsSessionAccumulator session =
          sessions.computeIfAbsent(sessionId, LandingAnalyticsSessionAccumulator::new);
      session.record(
          row.occurredAt(),
          eventType,
          sectionId,
          elapsedMs,
          pageUrl,
          userAgent,
          deviceType,
          operatingSystem,
          screenWidth,
          screenHeight,
          traffic);
    }

    Map<String, LandingAnalyticsSessionAccumulator> humanSessions =
        sessions.entrySet().stream()
            .filter(entry -> entry.getValue().isHuman())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (left, right) -> left,
                    LinkedHashMap::new));
    long pageViews = humanSessions.values().stream().mapToLong(s -> s.pageViews).sum();
    long sectionViewEvents =
        humanSessions.values().stream().mapToLong(s -> s.sectionViewEvents).sum();
    long totalVisibleMs = humanSessions.values().stream().mapToLong(s -> s.totalVisibleMs).sum();
    Instant lastEventAt =
        humanSessions.values().stream()
            .map(LandingAnalyticsSessionAccumulator::lastEventAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);

    List<ExperimentLandingAnalyticsSessionDto> sessionDtos =
        sessions.values().stream()
            .sorted(
                Comparator.comparing(
                    LandingAnalyticsSessionAccumulator::lastEventAt,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(50)
            .map(LandingAnalyticsSessionAccumulator::toDto)
            .toList();
    long averageVisibleMsPerSession =
        humanSessions.isEmpty() ? 0 : totalVisibleMs / humanSessions.size();
    List<ExperimentLandingAnalyticsDeviceDto> deviceBreakdown = buildDeviceBreakdown(humanSessions);
    List<ExperimentLandingAnalyticsOperatingSystemDto> mobileOperatingSystemBreakdown =
        buildMobileOperatingSystemBreakdown(humanSessions);
    List<ExperimentLandingAnalyticsScreenSizeDto> screenSizeBreakdown =
        buildScreenSizeBreakdown(humanSessions);
    ExperimentLandingAnalyticsLoadMetricDto loadMetrics = buildLoadMetrics(rows, humanSessions);
    ExperimentLandingAnalyticsVisitorsDto visitors =
        summarizeLandingAnalyticsVisitors(experiment, baseline);
    return new ExperimentLandingAnalyticsDto(
        humanSessions.values().stream().mapToLong(s -> s.eventCount).sum(),
        humanSessions.size(),
        pageViews,
        sectionViewEvents,
        totalVisibleMs,
        averageVisibleMsPerSession,
        lastEventAt,
        deviceBreakdown,
        mobileOperatingSystemBreakdown,
        screenSizeBreakdown,
        loadMetrics,
        visitors,
        new ExperimentLandingAnalyticsTrafficQualityDto(
            humanSessions.size(),
            sessions.values().stream()
                .filter(s -> s.trafficQuality == TrafficQuality.AUTOMATED)
                .count(),
            sessions.values().stream()
                .filter(s -> s.trafficQuality == TrafficQuality.UNKNOWN)
                .count()),
        sessionDtos);
  }

  /** Entrega ao Operador resumo, jornadas e eventos detalhados anonimizados do experimento. */
  public ExperimentLandingAnalyticsEvidenceDto buildDetailedAnalyticsEvidence(
      Long experimentId, int requestedLimit) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    boolean fakeExperiment = isFakeExperiment(experiment);
    Instant baseline = resolveBaseline(experiment);
    int limit = Math.max(1, Math.min(requestedLimit, 2000));
    var events =
        eventRepository.findLandingAnalyticsEvents(
            experimentId,
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            baseline,
            PageRequest.of(0, limit));
    long total =
        eventRepository.countLandingAnalyticsEvents(
            experimentId, ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE, baseline);
    List<ExperimentLandingAnalyticsDetailedEventDto> detailedEvents =
        events.stream()
            .sorted(
                Comparator.comparing(
                        ExperimentFunnelEventRepository.LandingAnalyticsEventProjection
                            ::getOccurredAt)
                    .thenComparing(
                        ExperimentFunnelEventRepository.LandingAnalyticsEventProjection::getId))
            .map(
                event -> {
                  Map<String, String> attributes =
                      new LinkedHashMap<>(parseDelimitedPayload(event.getPayload()));
                  String visitorId = attributes.remove("visitorId");
                  String sessionId = attributes.remove("sessionId");
                  TrafficDiagnosis traffic =
                      classifyLandingTraffic(attributes, visitorId, fakeExperiment);
                  attributes.remove("clientIp");
                  attributes.remove("userAgent");
                  attributes.remove("eventId");
                  return new ExperimentLandingAnalyticsDetailedEventDto(
                      event.getId(),
                      anonymousIdentifier("visitor", visitorId),
                      anonymousIdentifier("session", sessionId),
                      attributes.remove("eventType"),
                      attributes.remove("sectionId"),
                      event.getOccurredAt(),
                      traffic.quality().name(),
                      traffic.reason(),
                      attributes);
                })
            .toList();
    return new ExperimentLandingAnalyticsEvidenceDto(
        experimentId,
        total,
        detailedEvents.size(),
        total > detailedEvents.size(),
        sanitizeAnalyticsSummary(summarizeLandingAnalytics(experimentId)),
        detailedEvents);
  }

  /** Consolida geração, entrega e abertura das microamostras para decisões do Operador. */
  public Map<String, Object> buildPersonalizedSampleDeliveryEvidence(Long experimentId) {
    Map<String, Object> metrics =
        jdbcTemplate.queryForMap(
            """
                SELECT COUNT(*) AS requestedPackages,
                       SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedPackages,
                       COALESCE(SUM(p.planned_outputs), 0) AS plannedImages,
                       COALESCE(SUM((SELECT COUNT(*) FROM flow_submission_image_item i WHERE i.package_id = p.id)), 0) AS generatedImages,
                       SUM(CASE WHEN p.zip_generated_at IS NOT NULL THEN 1 ELSE 0 END) AS packagesWithZip,
                       SUM(CASE WHEN p.notified_at IS NOT NULL THEN 1 ELSE 0 END) AS deliveredEmails,
                       SUM(CASE WHEN p.email_opened_at IS NOT NULL THEN 1 ELSE 0 END) AS openedEmails,
                       MAX(p.updated_at) AS lastPackageUpdateAt
                FROM flow_submission_image_package p
                JOIN flow_submissions s ON s.id = p.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE %s
                """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId);
    LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("available", true);
    evidence.put("experimentId", experimentId);
    evidence.putAll(metrics);
    evidence.put("generationCostAvailable", false);
    evidence.put("generationCostUnavailableReason", "PACKAGE_COST_FIELDS_NOT_CANONICAL");
    evidence.put("scope", "TECHNICAL_AUDIT_NOT_HUMAN_SALES");
    return evidence;
  }

  /** Entrega jornadas PDE detalhadas e anonimizadas quando o experimento usa essa experiência. */
  public Map<String, Object> buildDetailedPdeAnalyticsEvidence(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    if (!isPdeMembershipSubscriptionFunnel(experiment)) {
      return Map.of("available", false, "reason", "NOT_PDE_EXPERIENCE");
    }
    try {
      PdeAnalyticsSummary summary = fetchPdeSummaryForExperiment(experiment);
      LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("available", true);
      evidence.put("productSlug", summary.productSlug());
      evidence.put("experienceVersion", summary.currentExperienceVersion());
      evidence.put("totalEvents", summary.totalEvents());
      evidence.put("rawTotalEvents", summary.rawTotalEvents());
      evidence.put("humanSessions", summary.humanSessions());
      evidence.put("botSuspectedSessions", summary.botSuspectedSessions());
      evidence.put("platformCrawlerSessions", summary.platformCrawlerSessions());
      evidence.put("internalQaSessions", summary.internalQaSessions());
      evidence.put("unknownSessions", summary.unknownSessions());
      evidence.put("eventMetrics", summary.events());
      evidence.put("experienceVersions", summary.experienceVersions());
      evidence.put("trafficSources", summary.trafficSources());
      evidence.put("trafficQuality", summary.trafficQualityBreakdown());
      evidence.put("devices", summary.deviceBreakdown());
      evidence.put("screenSizes", summary.screenSizeBreakdown());
      evidence.put(
          "detailedJourneys",
          summary.recentJourneys().stream().map(this::sanitizePdeJourney).toList());
      return evidence;
    } catch (Exception ex) {
      log.error(
          "Falha no modulo growth-operator ao coletar jornadas PDE; experimentId={}",
          experimentId,
          ex);
      return Map.of(
          "available",
          false,
          "reason",
          "PDE_ANALYTICS_ERROR",
          "errorType",
          ex.getClass().getSimpleName());
    }
  }

  /** Remove IP, user-agent e identificadores completos de uma jornada PDE detalhada. */
  private Map<String, Object> sanitizePdeJourney(PdeAnalyticsSummary.PdeSessionJourney journey) {
    LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
    safe.put("anonymousVisitorId", anonymousIdentifier("visitor", journey.visitorId()));
    safe.put("anonymousSessionId", anonymousIdentifier("session", journey.sessionId()));
    safe.put("trafficQuality", journey.trafficQuality());
    safe.put("trafficQualityReason", journey.trafficQualityReason());
    safe.put("trafficProvider", journey.trafficProvider());
    safe.put("firstEventAt", journey.firstEventAt());
    safe.put("lastEventAt", journey.lastEventAt());
    safe.put("totalVisibleMs", journey.totalVisibleMs());
    safe.put("maxScrollDepthPercent", journey.maxScrollDepthPercent());
    safe.put("screenNames", journey.screenNames());
    safe.put("sectionIds", journey.sectionIds());
    safe.put("fieldFocused", journey.fieldFocused());
    safe.put("fieldInputStarted", journey.fieldInputStarted());
    safe.put("fieldFilled", journey.fieldFilled());
    safe.put("ctaClicked", journey.ctaClicked());
    safe.put("loginStarted", journey.loginStarted());
    safe.put("loginCompleted", journey.loginCompleted());
    safe.put("paywallViewed", journey.paywallViewed());
    safe.put("checkoutStarted", journey.checkoutStarted());
    safe.put("subscriptionApproved", journey.subscriptionApproved());
    safe.put("abandonmentPoint", journey.abandonmentPoint());
    safe.put("lastEventType", journey.lastEventType());
    safe.put("lastActionName", journey.lastActionName());
    return safe;
  }

  /** Remove identificadores e user-agent bruto das jornadas agregadas entregues ao agente. */
  private ExperimentLandingAnalyticsDto sanitizeAnalyticsSummary(
      ExperimentLandingAnalyticsDto summary) {
    List<ExperimentLandingAnalyticsSessionDto> sessions =
        summary.sessions().stream()
            .map(
                session ->
                    new ExperimentLandingAnalyticsSessionDto(
                        anonymousIdentifier("session", session.sessionId()),
                        session.eventCount(),
                        session.pageViews(),
                        session.sectionViewEvents(),
                        session.totalVisibleMs(),
                        session.firstEventAt(),
                        session.lastEventAt(),
                        session.lastPageUrl(),
                        null,
                        session.deviceType(),
                        session.deviceLabel(),
                        session.operatingSystem(),
                        session.operatingSystemLabel(),
                        session.screenWidth(),
                        session.screenHeight(),
                        session.screenSizeLabel(),
                        session.trafficQuality(),
                        session.trafficQualityReason(),
                        session.topSections()))
            .toList();
    var visitors = summary.visitors();
    var safeVisitors =
        new ExperimentLandingAnalyticsVisitorsDto(
            visitors.probableVisitors(),
            visitors.recurrentVisitors(),
            visitors.singleVisitVisitors(),
            visitors.visitors().stream()
                .map(
                    visitor ->
                        new ExperimentLandingAnalyticsVisitorDto(
                            anonymousIdentifier("visitor", visitor.visitorId()),
                            visitor.totalSessions(),
                            visitor.validPageViews(),
                            visitor.firstAccessAt(),
                            visitor.lastAccessAt(),
                            visitor.intervalSeconds(),
                            visitor.distinctPages(),
                            null,
                            visitor.deviceType(),
                            visitor.deviceLabel(),
                            visitor.recurrent()))
                .toList());
    return new ExperimentLandingAnalyticsDto(
        summary.totalEvents(),
        summary.totalSessions(),
        summary.pageViews(),
        summary.sectionViewEvents(),
        summary.totalVisibleMs(),
        summary.averageVisibleMsPerSession(),
        summary.lastEventAt(),
        summary.deviceBreakdown(),
        summary.mobileOperatingSystemBreakdown(),
        summary.screenSizeBreakdown(),
        summary.loadMetrics(),
        safeVisitors,
        summary.trafficQuality(),
        sessions);
  }

  /** Gera pseudônimo estável por experimento sem expor identificadores first-party. */
  private String anonymousIdentifier(String prefix, String value) {
    if (value == null || value.isBlank()) {
      return "sem-" + prefix;
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((prefix + ":" + value.trim()).getBytes(StandardCharsets.UTF_8));
      return prefix + "-" + java.util.HexFormat.of().formatHex(digest, 0, 6);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Falha ao anonimizar identificador de analytics; tipo={}", prefix, ex);
      throw new IllegalStateException("SHA-256 indisponível para anonimização", ex);
    }
  }

  /**
   * Consolida visitantes prováveis recorrentes da landing para apoiar decisão comercial do
   * experimento.
   */
  public ExperimentLandingAnalyticsVisitorsDto summarizeLandingAnalyticsVisitors(
      Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    Instant baseline = resolveBaseline(experiment);
    return summarizeLandingAnalyticsVisitors(experiment, baseline);
  }

  /** Agrega visitantes prováveis a partir dos page_views normalizados já deduplicados. */
  private ExperimentLandingAnalyticsVisitorsDto summarizeLandingAnalyticsVisitors(
      Experiment experiment, Instant baseline) {
    List<ExperimentLandingAnalyticsVisitorDto> visitors =
        landingAnalyticsEventRepository
            .aggregateVisitorsByExperiment(experiment.getId(), baseline)
            .stream()
            .map(this::toVisitorDto)
            .toList();
    long recurrentVisitors =
        visitors.stream().filter(ExperimentLandingAnalyticsVisitorDto::recurrent).count();
    return new ExperimentLandingAnalyticsVisitorsDto(
        visitors.size(), recurrentVisitors, visitors.size() - recurrentVisitors, visitors);
  }

  /** Converte a agregação SQL em DTO público, mascarando o visitorId antes de responder à API. */
  private ExperimentLandingAnalyticsVisitorDto toVisitorDto(
      ExperimentLandingAnalyticsEventRepository.VisitorRecurrenceProjection projection) {
    Instant firstAccessAt = fromUtcDatabaseValue(projection.getFirstAccessAt());
    Instant lastAccessAt = fromUtcDatabaseValue(projection.getLastAccessAt());
    long intervalSeconds = secondsBetween(firstAccessAt, lastAccessAt);
    boolean recurrent =
        projection.getTotalSessions() >= 2
            || (projection.getValidPageViews() >= 2
                && intervalSeconds > PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS);
    String deviceType = normalizeLandingAnalyticsDeviceType(null, projection.getLastUserAgent());
    return new ExperimentLandingAnalyticsVisitorDto(
        maskVisitorId(projection.getVisitorId()),
        projection.getTotalSessions(),
        projection.getValidPageViews(),
        firstAccessAt,
        lastAccessAt,
        intervalSeconds,
        projection.getDistinctPages(),
        projection.getLastUserAgent(),
        deviceType,
        landingAnalyticsDeviceLabel(deviceType),
        recurrent);
  }

  /**
   * Calcula o intervalo em segundos entre primeiro e último acesso válido do visitante provável.
   */
  private long secondsBetween(Instant firstAccessAt, Instant lastAccessAt) {
    if (firstAccessAt == null || lastAccessAt == null) {
      return 0;
    }
    return Math.max(0, Duration.between(firstAccessAt, lastAccessAt).getSeconds());
  }

  /**
   * Mascara o identificador first-party para evitar exposição completa em resposta administrativa.
   */
  private String maskVisitorId(String visitorId) {
    if (visitorId == null || visitorId.isBlank()) {
      return "sem-visitante";
    }
    String normalized = visitorId.trim();
    if (normalized.length() <= 8) {
      return normalized.charAt(0) + "…" + normalized.charAt(normalized.length() - 1);
    }
    return normalized.substring(0, 4) + "…" + normalized.substring(normalized.length() - 4);
  }

  /** Registra manualmente um evento do funil para o experimento informado. */
  @Transactional
  public void registerEvent(Long experimentId, RegisterExperimentFunnelEventRequest request) {
    if (request == null || request.stage() == null) {
      throw new IllegalArgumentException("Etapa do funil é obrigatória");
    }
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    Lead lead = resolveLead(request.leadId());

    ExperimentFunnelEvent event =
        ExperimentFunnelEvent.builder()
            .experiment(experiment)
            .lead(lead)
            .stage(request.stage())
            .source(Optional.ofNullable(request.source()).orElse("manual"))
            .campaignCode(normalizeCampaignCode(request.campaignCode()))
            .payload(request.payload())
            .occurredAt(Optional.ofNullable(request.occurredAt()).orElse(Instant.now()))
            .build();
    eventRepository.save(event);
  }

  /**
   * Apaga eventos de teste do funil, incluindo analytics de sessão, e define o marco temporal de
   * reinício.
   */
  @Transactional
  public Instant resetFunnel(Long experimentId) {
    Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
    int deletedNormalizedLandingAnalytics =
        landingAnalyticsEventRepository.deleteByExperimentId(experimentId);
    int deletedLandingAnalytics =
        eventRepository.deleteByExperimentIdAndSource(
            experimentId, ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
    int deletedRemainingEvents = eventRepository.deleteByExperimentId(experimentId);
    Instant now = Instant.now();
    experiment.setFunnelResetAt(now);
    experimentRepository.save(experiment);
    log.info(
        "experiment_funnel_reset experimentId={} deletedNormalizedLandingAnalytics={} deletedLandingAnalytics={} deletedRemainingEvents={} resetAt={}",
        experimentId,
        deletedNormalizedLandingAnalytics,
        deletedLandingAnalytics,
        deletedRemainingEvents,
        now);
    return now;
  }

  /** Registra a visualização do formulário recebida pelo tracking público do Lead Portal. */
  @Transactional
  public void registerFormRenderCompleted(String flowSlug, String visitorId, String campaignCode) {
    if (flowSlug == null || flowSlug.isBlank()) {
      throw new IllegalArgumentException("Slug do fluxo é obrigatório");
    }
    Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

    String sanitizedVisitorId = visitorId == null ? null : visitorId.trim();
    String payload =
        sanitizedVisitorId == null || sanitizedVisitorId.isBlank()
            ? null
            : "visitorId=" + sanitizedVisitorId;

    ExperimentFunnelEvent event =
        ExperimentFunnelEvent.builder()
            .experiment(experiment)
            .stage(ExperimentFunnelStage.VISUALIZACAO_FORM)
            .source(ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE)
            .campaignCode(normalizeCampaignCode(campaignCode))
            .payload(payload)
            .occurredAt(Instant.now())
            .build();
    eventRepository.save(event);
  }

  /**
   * Registra de forma idempotente o envio do formulário recebido pelo Lead Portal e aciona standby
   * no primeiro envio válido.
   */
  @Transactional
  public boolean registerFormSubmission(
      String flowSlug, RegisterLeadPortalSubmissionRequest request) {
    if (flowSlug == null || flowSlug.isBlank()) {
      throw new IllegalArgumentException("Slug do fluxo é obrigatório");
    }
    if (request == null || request.submissionId() == null || request.submissionId().isBlank()) {
      throw new IllegalArgumentException("ID da submissão é obrigatório");
    }

    Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

    String payload = "submissionId=" + request.submissionId().trim();
    boolean duplicated =
        eventRepository.existsByExperimentIdAndStageAndSourceAndPayload(
            experiment.getId(),
            ExperimentFunnelStage.ENVIO_FORM,
            ExperimentFunnelEventRepository.SUBMISSION_SOURCE,
            payload);
    if (duplicated) {
      return false;
    }
    Instant occurredAt = Optional.ofNullable(request.submittedAt()).orElse(Instant.now());

    ExperimentFunnelEvent event =
        ExperimentFunnelEvent.builder()
            .experiment(experiment)
            .stage(ExperimentFunnelStage.ENVIO_FORM)
            .source(ExperimentFunnelEventRepository.SUBMISSION_SOURCE)
            .campaignCode(normalizeCampaignCode(request.campaignCode()))
            .payload(payload)
            .occurredAt(occurredAt)
            .build();
    eventRepository.save(event);
    if (!isInternalTestCampaign(request.campaignCode())) {
      standbyService.standbyOnFirstValidFormSubmission(experiment);
    }
    return true;
  }

  /** Converte o contrato v1 de submissão do Lead Portal para o registro idempotente legado. */
  @Transactional
  public boolean registerFormSubmission(
      String flowSlug, LeadPortalSubmissionEngagementContractV1 request) {
    if (request == null) {
      throw new IllegalArgumentException("Payload de submissão é obrigatório");
    }
    if (!LeadPortalSubmissionEngagementContractV1.VERSION.equals(request.contractVersion())) {
      throw new IllegalArgumentException(
          "Versão de contrato não suportada: " + request.contractVersion());
    }
    RegisterLeadPortalSubmissionRequest legacyRequest =
        new RegisterLeadPortalSubmissionRequest(
            request.submissionId(), request.submittedAt(), request.campaignCode());
    return registerFormSubmission(flowSlug, legacyRequest);
  }

  /**
   * Registra analytics da landing publicada, preserva o evento bruto e normaliza o contrato por
   * visitante provável.
   */
  @Transactional
  public void registerLandingPageAnalyticsEvent(
      String flowSlug, RegisterLandingPageAnalyticsEventRequest request) {
    if (flowSlug == null || flowSlug.isBlank()) {
      throw new IllegalArgumentException("Slug do fluxo é obrigatório");
    }
    log.info("landing_page_analytics_raw flowSlug={} payload={}", flowSlug.trim(), request);
    validateLandingAnalyticsRequest(request);
    if (internalAnalyticsTrafficFilter.isInternal(request.clientIp())) {
      log.info(
          "landing_page_analytics_internal_traffic_ignored flowSlug={} eventId={} clientIp={}",
          flowSlug.trim(),
          request.eventId().trim(),
          request.clientIp().trim());
      return;
    }
    Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

    Long elapsedMs = resolveLandingAnalyticsElapsedMs(request);
    String eventType = request.eventType().trim();
    Instant occurredAt = Optional.ofNullable(request.occurredAt()).orElse(Instant.now());
    String payload = buildLandingAnalyticsPayload(request, elapsedMs);

    log.info(
        "landing_page_analytics experimentId={} flowSlug={} eventId={} visitorId={} eventType={} sectionId={} elapsedMs={} sessionId={} pageUrl={} occurredAt={} userAgent={} deviceType={} clientIp={}",
        experiment.getId(),
        flowSlug.trim(),
        request.eventId().trim(),
        request.visitorId(),
        eventType,
        request.sectionId(),
        elapsedMs,
        request.sessionId(),
        request.pageUrl(),
        occurredAt,
        request.userAgent(),
        normalizeLandingAnalyticsDeviceType(request.deviceType(), request.userAgent()),
        request.clientIp());

    ExperimentFunnelStage stage = resolveStageForLandingAnalyticsEvent(experiment, eventType);
    if (stage != null) {
      ExperimentFunnelEvent event =
          ExperimentFunnelEvent.builder()
              .experiment(experiment)
              .stage(stage)
              .source(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE)
              .campaignCode(null)
              .payload(payload)
              .occurredAt(occurredAt)
              .build();
      ExperimentFunnelEvent savedEvent = eventRepository.save(event);
      saveNormalizedLandingAnalyticsEvent(experiment, savedEvent, request, occurredAt, payload);
    }
  }

  /**
   * Valida campos obrigatórios do contrato público de analytics conforme o tipo do evento recebido.
   */
  private void validateLandingAnalyticsRequest(RegisterLandingPageAnalyticsEventRequest request) {
    if (request == null || request.eventId() == null || request.eventId().isBlank()) {
      throw new IllegalArgumentException("eventId é obrigatório");
    }
    if (request.eventType() == null || request.eventType().isBlank()) {
      throw new IllegalArgumentException("eventType é obrigatório");
    }
    String eventType = request.eventType().trim();
    if ("page_view".equalsIgnoreCase(eventType)) {
      validateRequiredLandingAnalyticsField(
          request.sessionId(), "sessionId é obrigatório para page_view");
      validateRequiredLandingAnalyticsField(
          request.pageUrl(), "pageUrl é obrigatório para page_view");
    }
    if ("section_view_time".equalsIgnoreCase(eventType)) {
      validateRequiredLandingAnalyticsField(
          request.sessionId(), "sessionId é obrigatório para section_view_time");
      validateRequiredLandingAnalyticsField(
          request.sectionId(), "sectionId é obrigatório para section_view_time");
      if (resolveLandingAnalyticsElapsedMs(request) == null) {
        throw new IllegalArgumentException("elapsedMs é obrigatório para section_view_time");
      }
    }
    if ("page_load_metric".equalsIgnoreCase(eventType)) {
      validateRequiredLandingAnalyticsField(
          request.sessionId(), "sessionId é obrigatório para page_load_metric");
      validateRequiredLandingAnalyticsField(
          request.pageUrl(), "pageUrl é obrigatório para page_load_metric");
    }
    if ("checkout_click".equalsIgnoreCase(eventType)) {
      validateRequiredLandingAnalyticsField(
          request.sessionId(), "sessionId é obrigatório para checkout_click");
      validateRequiredLandingAnalyticsField(
          request.pageUrl(), "pageUrl é obrigatório para checkout_click");
    }
    if ("video_progress".equalsIgnoreCase(eventType)
        || "video_complete".equalsIgnoreCase(eventType)) {
      validateRequiredLandingAnalyticsField(
          request.sessionId(), "sessionId é obrigatório para eventos de vídeo");
      validateRequiredLandingAnalyticsField(
          request.pageUrl(), "pageUrl é obrigatório para eventos de vídeo");
    }
    if ("form_start".equalsIgnoreCase(eventType) || "form_submit".equalsIgnoreCase(eventType)) {
      validateRequiredLandingAnalyticsField(
          request.sessionId(), "sessionId é obrigatório para eventos de formulário");
      validateRequiredLandingAnalyticsField(
          request.pageUrl(), "pageUrl é obrigatório para eventos de formulário");
    }
  }

  /** Rejeita campos textuais obrigatórios quando estão ausentes ou em branco. */
  private void validateRequiredLandingAnalyticsField(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }

  /** Resolve a duração canônica do evento mantendo compatibilidade com o campo legado visibleMs. */
  private Long resolveLandingAnalyticsElapsedMs(RegisterLandingPageAnalyticsEventRequest request) {
    return request.elapsedMs() != null ? request.elapsedMs() : request.visibleMs();
  }

  /**
   * Salva ou atualiza o evento normalizado, ignorando page_view duplicado na janela canônica curta.
   */
  private void saveNormalizedLandingAnalyticsEvent(
      Experiment experiment,
      ExperimentFunnelEvent funnelEvent,
      RegisterLandingPageAnalyticsEventRequest request,
      Instant occurredAt,
      String payload) {
    String visitorId = normalizeNullableText(request.visitorId(), 128);
    String sessionId = normalizeNullableText(request.sessionId(), 128);
    String eventType = normalizeNullableText(request.eventType(), 64);
    String pageUrl = normalizeNullableText(request.pageUrl(), 2048);
    if (isDuplicatedPageView(
        experiment.getId(), request, visitorId, sessionId, eventType, pageUrl, occurredAt)) {
      log.info(
          "landing_page_analytics_deduplicated experimentId={} eventId={} visitorId={} sessionId={} eventType={} pageUrl={} occurredAt={} windowSeconds={}",
          experiment.getId(),
          request.eventId().trim(),
          visitorId,
          sessionId,
          eventType,
          pageUrl,
          occurredAt,
          PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS);
      return;
    }
    ExperimentLandingAnalyticsEvent normalizedEvent =
        landingAnalyticsEventRepository
            .findFirstByExperimentIdAndEventId(experiment.getId(), request.eventId().trim())
            .orElseGet(ExperimentLandingAnalyticsEvent::new);
    normalizedEvent.setExperiment(experiment);
    normalizedEvent.setFunnelEvent(funnelEvent);
    normalizedEvent.setEventId(normalizeNullableText(request.eventId(), 128));
    normalizedEvent.setVisitorId(visitorId);
    normalizedEvent.setSessionId(sessionId);
    normalizedEvent.setEventType(eventType);
    normalizedEvent.setSectionId(normalizeNullableText(request.sectionId(), 190));
    normalizedEvent.setPageUrl(pageUrl);
    normalizedEvent.setUserAgent(normalizeNullableText(request.userAgent(), 512));
    TrafficDiagnosis traffic =
        classifyLandingTraffic(
            parseDelimitedPayload(payload), visitorId, isFakeExperiment(experiment));
    normalizedEvent.setTrafficQuality(traffic.quality().name());
    normalizedEvent.setTrafficQualityReason(traffic.reason());
    normalizedEvent.setOccurredAt(occurredAt);
    landingAnalyticsEventRepository.save(normalizedEvent);
  }

  /**
   * Verifica se o evento é um page_view duplicado por visitante, sessão e URL dentro da janela
   * canônica.
   */
  private boolean isDuplicatedPageView(
      Long experimentId,
      RegisterLandingPageAnalyticsEventRequest request,
      String visitorId,
      String sessionId,
      String eventType,
      String pageUrl,
      Instant occurredAt) {
    if (!"page_view".equalsIgnoreCase(eventType)
        || visitorId == null
        || sessionId == null
        || pageUrl == null) {
      return false;
    }
    if (landingAnalyticsEventRepository
        .findFirstByExperimentIdAndEventId(experimentId, request.eventId().trim())
        .isPresent()) {
      return false;
    }
    return landingAnalyticsEventRepository.existsPageViewInDeduplicationWindow(
        experimentId,
        visitorId,
        sessionId,
        eventType,
        pageUrl,
        occurredAt.minusSeconds(PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS),
        occurredAt.plusSeconds(PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS));
  }

  /** Normaliza texto para colunas relacionais, retornando null quando o valor está vazio. */
  private String normalizeNullableText(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().replace("\r", " ").replace("\n", " ");
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }

  /**
   * Resolve o experimento pelo slug, cobrindo tanto vínculo interno quanto landing publicada
   * externamente.
   */
  private Experiment resolveExperimentByFlowSlug(String flowSlug) {
    return experimentRepository
        .findFirstByLeadPortalFlowSlug(flowSlug)
        .or(() -> experimentRepository.findFirstByFollowUpActionUrlFlowSlug(flowSlug))
        .orElseThrow(() -> new IllegalArgumentException("Fluxo não vinculado a experimento"));
  }

  /** Mapeia eventos de analytics da landing para a etapa consolidada do funil. */
  private ExperimentFunnelStage resolveStageForLandingAnalyticsEvent(
      Experiment experiment, String eventType) {
    if ("page_view".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.VISUALIZACAO_FORM;
    }
    if ("section_view_time".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.VISUALIZACAO_FORM;
    }
    if ("page_load_metric".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.VISUALIZACAO_FORM;
    }
    if ("form_start".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.VISUALIZACAO_FORM;
    }
    if ("form_submit".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.ENVIO_FORM;
    }
    if ("video_progress".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.VIDEO_VISTO_PARCIAL;
    }
    if ("video_complete".equalsIgnoreCase(eventType)) {
      return ExperimentFunnelStage.VIDEO_VISTO_COMPLETO;
    }
    if ("checkout_click".equalsIgnoreCase(eventType) && isPurchaseIntentFunnel(experiment)) {
      return ExperimentFunnelStage.ACESSO_CHECKOUT;
    }
    return null;
  }

  /** Monta um payload textual rastreável sem serializar JSON dentro de JSON. */
  private String buildLandingAnalyticsPayload(
      RegisterLandingPageAnalyticsEventRequest request, Long elapsedMs) {
    String sectionId = sanitizePayloadValue(request.sectionId());
    String visitorId = sanitizePayloadValue(request.visitorId());
    String sessionId = sanitizePayloadValue(request.sessionId());
    String url = sanitizePayloadValue(request.pageUrl());
    String userAgent = sanitizePayloadValue(request.userAgent());
    String deviceType =
        sanitizePayloadValue(
            normalizeLandingAnalyticsDeviceType(request.deviceType(), request.userAgent()));
    String operatingSystem =
        sanitizePayloadValue(
            normalizeLandingAnalyticsOperatingSystem(
                request.operatingSystem(), request.userAgent()));
    String screenWidth = request.screenWidth() == null ? "" : request.screenWidth().toString();
    String screenHeight = request.screenHeight() == null ? "" : request.screenHeight().toString();
    String loadDurationMs =
        request.loadDurationMs() == null ? "" : request.loadDurationMs().toString();
    String domContentLoadedMs =
        request.domContentLoadedMs() == null ? "" : request.domContentLoadedMs().toString();
    String firstContentfulPaintMs =
        request.firstContentfulPaintMs() == null ? "" : request.firstContentfulPaintMs().toString();
    String largestContentfulPaintMs =
        request.largestContentfulPaintMs() == null
            ? ""
            : request.largestContentfulPaintMs().toString();
    String cumulativeLayoutShift =
        request.cumulativeLayoutShift() == null ? "" : request.cumulativeLayoutShift().toString();
    String interactionToNextPaintMs =
        request.interactionToNextPaintMs() == null
            ? ""
            : request.interactionToNextPaintMs().toString();
    String timeToFirstByteMs =
        request.timeToFirstByteMs() == null ? "" : request.timeToFirstByteMs().toString();
    String resourceErrorCount =
        request.resourceErrorCount() == null ? "" : request.resourceErrorCount().toString();
    String connectionType = sanitizePayloadValue(request.connectionType());
    String clientIp = sanitizePayloadValue(request.clientIp());
    String automationSignal =
        request.automationSignal() == null ? "" : request.automationSignal().toString();
    String referrer = sanitizePayloadValue(request.referrer());
    String videoId = sanitizePayloadValue(request.videoId());
    String videoCurrentTimeMs =
        request.videoCurrentTimeMs() == null ? "" : request.videoCurrentTimeMs().toString();
    String videoDurationMs =
        request.videoDurationMs() == null ? "" : request.videoDurationMs().toString();
    String videoPercent = request.videoPercent() == null ? "" : request.videoPercent().toString();
    String duration = elapsedMs == null ? "" : elapsedMs.toString();
    return "eventId="
        + sanitizePayloadValue(request.eventId())
        + ";eventType="
        + sanitizePayloadValue(request.eventType())
        + ";visitorId="
        + visitorId
        + ";sessionId="
        + sessionId
        + ";sectionId="
        + sectionId
        + ";elapsedMs="
        + duration
        + ";pageUrl="
        + url
        + ";userAgent="
        + userAgent
        + ";deviceType="
        + deviceType
        + ";operatingSystem="
        + operatingSystem
        + ";screenWidth="
        + sanitizePayloadValue(screenWidth)
        + ";screenHeight="
        + sanitizePayloadValue(screenHeight)
        + ";loadDurationMs="
        + sanitizePayloadValue(loadDurationMs)
        + ";domContentLoadedMs="
        + sanitizePayloadValue(domContentLoadedMs)
        + ";firstContentfulPaintMs="
        + sanitizePayloadValue(firstContentfulPaintMs)
        + ";largestContentfulPaintMs="
        + sanitizePayloadValue(largestContentfulPaintMs)
        + ";cumulativeLayoutShift="
        + sanitizePayloadValue(cumulativeLayoutShift)
        + ";interactionToNextPaintMs="
        + sanitizePayloadValue(interactionToNextPaintMs)
        + ";timeToFirstByteMs="
        + sanitizePayloadValue(timeToFirstByteMs)
        + ";resourceErrorCount="
        + sanitizePayloadValue(resourceErrorCount)
        + ";connectionType="
        + connectionType
        + ";clientIp="
        + clientIp
        + ";automationSignal="
        + sanitizePayloadValue(automationSignal)
        + ";referrer="
        + referrer
        + ";videoId="
        + videoId
        + ";videoCurrentTimeMs="
        + sanitizePayloadValue(videoCurrentTimeMs)
        + ";videoDurationMs="
        + sanitizePayloadValue(videoDurationMs)
        + ";videoPercent="
        + sanitizePayloadValue(videoPercent);
  }

  /** Resolve o lead opcional associado ao evento manual quando o identificador foi informado. */
  private Lead resolveLead(UUID leadId) {
    if (leadId == null) {
      return null;
    }
    return leadRepository.findById(leadId).orElse(null);
  }

  /** Inicializa todas as etapas do funil com contadores zerados para consolidação. */
  private Map<ExperimentFunnelStage, ExperimentFunnelStageDto> bootstrapStages() {
    Map<ExperimentFunnelStage, ExperimentFunnelStageDto> result = new LinkedHashMap<>();
    Arrays.stream(ExperimentFunnelStage.values())
        .forEach(
            stage -> {
              ExperimentFunnelStageDto dto = new ExperimentFunnelStageDto();
              dto.setStage(stage);
              dto.setLabel(stage.getLabel());
              dto.setOrder(stage.getOrder());
              dto.setAutoCount(0);
              dto.setManualCount(0);
              dto.setTotalCount(0);
              result.put(stage, dto);
            });
    return result;
  }

  /** Aplica os eventos manuais registrados no repositório às etapas consolidadas. */
  private void applyManualEvents(
      Long experimentId,
      Instant baseline,
      Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
    for (StageAggregation agg :
        eventRepository.aggregateManualByExperiment(experimentId, baseline)) {
      ExperimentFunnelStageDto dto = stages.get(agg.getStage());
      if (dto == null) {
        continue;
      }
      dto.setManualCount(agg.getTotal());
      dto.setTotalCount(dto.getTotalCount() + agg.getTotal());
      dto.setUniqueCount(sum(dto.getUniqueCount(), agg.getUniqueLeads()));
      dto.setLastEventAt(max(dto.getLastEventAt(), agg.getLastEvent()));
    }
  }

  /**
   * Aplica métricas automáticas vindas das tabelas operacionais e dos eventos públicos do funil.
   */
  private void applyAutomaticMetrics(
      Long experimentId,
      Instant baseline,
      Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
    LocalDateTime sqlBaseline = toUtcDatabaseDateTime(baseline);
    mergeMetric(
        stages,
        ExperimentFunnelStage.VISUALIZACAO_ANUNCIO,
        fetchSingleMetric(
            """
                        SELECT COALESCE(SUM(impressions), 0) AS total,
                               NULL AS unique_count,
                               MAX(updated_at) AS last_event
                        FROM experiment_campaign_metric
                        WHERE experiment_id = ?
                          AND (? IS NULL OR updated_at > ?)
                        """,
            experimentId,
            sqlBaseline,
            sqlBaseline),
        "Impressões vindas do Facebook Ads (experiment_campaign_metric)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.ACESSO_FORM_LEAD,
        fetchSingleMetric(
            """
                        SELECT COALESCE(SUM(clicks), 0) AS total,
                               NULL AS unique_count,
                               MAX(updated_at) AS last_event
                        FROM experiment_campaign_metric
                        WHERE experiment_id = ?
                          AND (? IS NULL OR updated_at > ?)
                        """,
            experimentId,
            sqlBaseline,
            sqlBaseline),
        "Cliques do anúncio para o formulário (experiment_campaign_metric)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.VISUALIZACAO_FORM,
        fetchSingleMetric(
            """
                        SELECT CASE
                                   WHEN normalized_page_views.all_events > 0 THEN normalized_page_views.total
                                   ELSE render_complete.total
                               END AS total,
                               NULL AS unique_count,
                               CASE
                                   WHEN normalized_page_views.all_events > 0 THEN normalized_page_views.last_event
                                   ELSE render_complete.last_event
                               END AS last_event
                        FROM (
                            SELECT COUNT(*) AS all_events,
                                   COALESCE(SUM(CASE WHEN traffic_quality = 'HUMAN' THEN 1 ELSE 0 END), 0) AS total,
                                   MAX(CASE WHEN traffic_quality = 'HUMAN' THEN occurred_at ELSE NULL END) AS last_event
                            FROM experiment_landing_analytics_event
                            WHERE experiment_id = ?
                              AND LOWER(event_type) = 'page_view'
                              AND LOWER(COALESCE(page_url, '')) NOT LIKE '%%mh_test=1%%'
                              AND LOWER(COALESCE(page_url, '')) NOT LIKE '%%mh_audit=%%'
                              AND (? IS NULL OR occurred_at > ?)
                        ) normalized_page_views
                        CROSS JOIN (
                            SELECT COUNT(*) AS total,
                                   MAX(occurred_at) AS last_event
                            FROM experiment_funnel_event
                            WHERE experiment_id = ?
                              AND stage = 'VISUALIZACAO_FORM'
                              AND source = ?
                              AND (? IS NULL OR occurred_at > ?)
                        ) render_complete
                        """,
            experimentId,
            sqlBaseline,
            sqlBaseline,
            experimentId,
            ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE,
            sqlBaseline,
            sqlBaseline),
        "Visualizações canônicas por page_view normalizado com fallback legado de render-complete");

    mergeMetric(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_PARCIAL,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT NULLIF(
                                   SUBSTRING_INDEX(SUBSTRING_INDEX(efe.payload, 'visitorId=', -1), ';', 1),
                                   ''
                               )) AS unique_count,
                               MAX(efe.occurred_at) AS last_event
                        FROM experiment_funnel_event efe
                        JOIN experiment_landing_analytics_event ela
                          ON ela.funnel_event_id = efe.id
                         AND ela.traffic_quality = 'HUMAN'
                        WHERE efe.experiment_id = ?
                          AND efe.stage = 'VIDEO_VISTO_PARCIAL'
                          AND efe.source = ?
                          AND LOWER(COALESCE(efe.payload, '')) NOT LIKE '%%mh_test=1%%'
                          AND LOWER(COALESCE(efe.payload, '')) NOT LIKE '%%mh_audit=%%'
                          AND (? IS NULL OR efe.occurred_at > ?)
                        """,
            experimentId,
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            sqlBaseline,
            sqlBaseline),
        "Vídeos vistos parcialmente no PDE ou página publicada (video_progress)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_COMPLETO,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT NULLIF(
                                   SUBSTRING_INDEX(SUBSTRING_INDEX(efe.payload, 'visitorId=', -1), ';', 1),
                                   ''
                               )) AS unique_count,
                               MAX(efe.occurred_at) AS last_event
                        FROM experiment_funnel_event efe
                        JOIN experiment_landing_analytics_event ela
                          ON ela.funnel_event_id = efe.id
                         AND ela.traffic_quality = 'HUMAN'
                        WHERE efe.experiment_id = ?
                          AND efe.stage = 'VIDEO_VISTO_COMPLETO'
                          AND efe.source = ?
                          AND LOWER(COALESCE(efe.payload, '')) NOT LIKE '%%mh_test=1%%'
                          AND LOWER(COALESCE(efe.payload, '')) NOT LIKE '%%mh_audit=%%'
                          AND (? IS NULL OR efe.occurred_at > ?)
                        """,
            experimentId,
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            sqlBaseline,
            sqlBaseline),
        "Vídeos vistos até o fim no PDE ou página publicada (video_complete)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.ENVIO_FORM,
        fetchSingleMetric(
            """
                        SELECT COUNT(DISTINCT canonical_submission_id) AS total,
                               COUNT(DISTINCT lead_id) AS unique_count,
                               MAX(submitted_at) AS last_event
                        FROM (
                            SELECT CAST(CONCAT('legacy:', lps.id) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   lps.lead_id,
                                   lps.submitted_at
                            FROM lead_portal_submission lps
                            WHERE lps.experiment_id = ?
                            UNION ALL
                            SELECT CAST(fs.id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   NULL AS lead_id,
                                   fs.created_at AS submitted_at
                            FROM flow_submissions fs
                            JOIN lead_portal_flow f ON f.slug = fs.flow_slug
                            WHERE %s
                              AND (fs.campaign_code IS NULL OR fs.campaign_code <> '__mh_internal_test__')
                            UNION ALL
                            SELECT CAST(SUBSTRING(efe.payload, CHAR_LENGTH('submissionId=') + 1) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   efe.lead_id,
                                   efe.occurred_at AS submitted_at
                            FROM experiment_funnel_event efe
                            WHERE efe.experiment_id = ?
                              AND efe.stage = 'ENVIO_FORM'
                              AND efe.source = ?
                              AND efe.payload LIKE 'submissionId=%%'
                              AND (efe.campaign_code IS NULL OR efe.campaign_code <> '__mh_internal_test__')
                        ) submissions
                        WHERE canonical_submission_id IS NOT NULL
                          AND canonical_submission_id <> ''
                          AND (? IS NULL OR submitted_at > ?)
                        """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId,
            experimentId,
            experimentId,
            ExperimentFunnelEventRepository.SUBMISSION_SOURCE,
            sqlBaseline,
            sqlBaseline),
        "Envios do formulário (lead_portal_submission + flow_submissions + experiment_funnel_event)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT s.id) AS unique_count,
                               MAX(p.email_opened_at) AS last_event
                        FROM flow_submission_image_package p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND (s.campaign_code IS NULL OR s.campaign_code <> '__mh_internal_test__')
                          AND p.email_opened_at IS NOT NULL
                          AND (? IS NULL OR p.email_opened_at > ?)
                        """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId,
            sqlBaseline,
            sqlBaseline),
        "Aberturas de e-mail de amostra (flow_submission_image_package.email_opened_at)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.ACESSO_CHECKOUT,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               NULL AS unique_count,
                               MAX(event_at) AS last_event
                        FROM (
                            SELECT p.checkout_accessed_at AS event_at
                            FROM lead_portal_purchase p
                            JOIN flow_submissions s ON s.id = p.submission_id
                            JOIN lead_portal_flow f ON f.slug = s.flow_slug
                            WHERE %s
                              AND (s.campaign_code IS NULL OR s.campaign_code <> '__mh_internal_test__')
                              AND p.checkout_accessed_at IS NOT NULL
                            UNION ALL
                            SELECT efe.occurred_at AS event_at
                            FROM experiment_funnel_event efe
                            JOIN experiment_landing_analytics_event ela
                              ON ela.funnel_event_id = efe.id
                             AND ela.traffic_quality = 'HUMAN'
                            WHERE efe.experiment_id = ?
                              AND efe.stage = 'ACESSO_CHECKOUT'
                              AND efe.source = ?
                              AND efe.payload LIKE '%%eventType=checkout_click%%'
                              AND LOWER(COALESCE(efe.payload, '')) NOT LIKE '%%mh_test=1%%'
                              AND LOWER(COALESCE(efe.payload, '')) NOT LIKE '%%mh_audit=%%'
                        ) checkout_access
                        WHERE (? IS NULL OR event_at > ?)
                        """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId,
            experimentId,
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            sqlBaseline,
            sqlBaseline),
        "Acessos ao checkout registrados via tracking público e clique real no checkout (checkout_click)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.COMPRA,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT p.submission_id) AS unique_count,
                               MAX(COALESCE(p.payment_approved_at, p.updated_at)) AS last_event
                        FROM lead_portal_purchase p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND (s.campaign_code IS NULL OR s.campaign_code <> '__mh_internal_test__')
                          AND (p.payment_approved_at IS NOT NULL OR p.mp_status = 'approved')
                          AND (? IS NULL OR COALESCE(p.payment_approved_at, p.updated_at) > ?)
                        """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId,
            sqlBaseline,
            sqlBaseline),
        "Pagamentos aprovados (lead_portal_purchase)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.ABERTURA_EMAIL_COMPRA,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT d.submission_id) AS unique_count,
                               MAX(el.opened_at) AS last_event
                        FROM lead_portal_premium_delivery d
                        JOIN flow_submissions s ON s.id = d.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        LEFT JOIN email_log el ON el.request_id = d.email_request_id
                        WHERE %s
                          AND (s.campaign_code IS NULL OR s.campaign_code <> '__mh_internal_test__')
                          AND d.email_request_id IS NOT NULL
                          AND el.opened_at IS NOT NULL
                          AND (? IS NULL OR el.opened_at > ?)
                        """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId,
            sqlBaseline,
            sqlBaseline),
        "Abertura do e-mail de entrega (lead_portal_premium_delivery -> email_log)");

    mergeMetric(
        stages,
        ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO,
        fetchSingleMetric(
            """
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT p.submission_id) AS unique_count,
                               MAX(p.images_viewed_at) AS last_event
                        FROM flow_submission_image_package p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND (s.campaign_code IS NULL OR s.campaign_code <> '__mh_internal_test__')
                          AND p.payment_purchase_id IS NOT NULL
                          AND p.images_viewed_at IS NOT NULL
                          AND (? IS NULL OR p.images_viewed_at > ?)
                        """
                .formatted(FLOW_SCOPE_CONDITION),
            experimentId,
            experimentId,
            sqlBaseline,
            sqlBaseline),
        "Downloads/visualizações do material pago (flow_submission_image_package.images_viewed_at)");
  }

  /**
   * Aplica métricas reais do backend PDE/MUSA ao funil de assinatura, preservando a atribuição por
   * campanha.
   */
  private void applyPdeMembershipMetrics(
      Experiment experiment, Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
    if (!isPdeMembershipSubscriptionFunnel(experiment)) {
      return;
    }
    PdeAnalyticsSummary summary;
    try {
      summary = fetchPdeSummaryForExperiment(experiment);
    } catch (Exception ex) {
      log.error(
          "Falha ao consultar analytics PDE para consolidar funil do experimento; experimentId={} productSlug={}",
          experiment.getId(),
          DEFAULT_PDE_PRODUCT_SLUG,
          ex);
      return;
    }
    if (summary == null) {
      return;
    }

    List<String> attributionCodes = fetchExperimentAttributionCodes(experiment);
    PdeMembershipMetric metric =
        aggregatePdeMembershipMetric(
            summary,
            attributionCodes,
            experiment.getFollowUpActionUrl(),
            !isFakeExperiment(experiment));
    mergeMetric(
        stages,
        ExperimentFunnelStage.VISUALIZACAO_FORM,
        new AggregatedMetric(metric.pdeEntries(), null, metric.lastEventAt()),
        "Entradas reais do PDE/MUSA filtradas por UTM da campanha do experimento");
    mergeMetric(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_PARCIAL,
        new AggregatedMetric(metric.videoPartial(), null, metric.lastEventAt()),
        "Consumo parcial do vídeo registrado no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_COMPLETO,
        new AggregatedMetric(metric.videoComplete(), null, metric.lastEventAt()),
        "Consumo completo do vídeo registrado no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.ENVIO_FORM,
        new AggregatedMetric(metric.loginStarted(), null, metric.lastEventAt()),
        "Logins ou criações de conta registrados no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA,
        new AggregatedMetric(metric.paywallViewed(), null, metric.lastEventAt()),
        "Visualizações de paywall/oferta registradas no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.ACESSO_CHECKOUT,
        new AggregatedMetric(metric.checkoutIntent(), null, metric.lastEventAt()),
        "Cliques em plano ou checkout registrados no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.COMPRA,
        new AggregatedMetric(metric.subscriptionApproved(), null, metric.lastEventAt()),
        "Assinaturas aprovadas registradas no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.ABERTURA_EMAIL_COMPRA,
        new AggregatedMetric(metric.accessReleased(), null, metric.lastEventAt()),
        "Liberações de acesso registradas no analytics PDE/MUSA");
    mergeMetric(
        stages,
        ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO,
        new AggregatedMetric(metric.firstUse(), null, metric.lastEventAt()),
        "Primeiro uso registrado no analytics PDE/MUSA");
  }

  /** Busca o resumo PDE correto para experimento real ou fake de diagnóstico técnico. */
  private PdeAnalyticsSummary fetchPdeSummaryForExperiment(Experiment experiment) {
    if (isFakeExperiment(experiment)) {
      return pdeAnalyticsClient.fetchSummaryIncludingNonHumanTraffic(
          DEFAULT_PDE_PRODUCT_SLUG, experiment.getFollowUpActionUrl());
    }
    return pdeAnalyticsClient.fetchSummary(
        DEFAULT_PDE_PRODUCT_SLUG, experiment.getFollowUpActionUrl());
  }

  /**
   * Busca os códigos Meta e UTMs persistidos para atribuir o analytics PDE ao experimento correto.
   */
  private List<String> fetchExperimentAttributionCodes(Experiment experiment) {
    Long experimentId = experiment.getId();
    List<String> persistedCodes =
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
    if (!isFakeExperiment(experiment)) {
      return persistedCodes;
    }
    List<String> fakeCodes = new ArrayList<>(persistedCodes);
    fakeCodes.add("mh_fake_exp_" + experimentId);
    fakeCodes.add("fake_exp_" + experimentId);
    fakeCodes.add("fake-" + experimentId);
    fakeCodes.add("exp-" + experimentId);
    fakeCodes.add("exp_" + experimentId);
    fakeCodes.add("experiment-" + experimentId);
    fakeCodes.add("experiment_" + experimentId);
    return fakeCodes.stream().distinct().toList();
  }

  /**
   * Agrega métricas PDE usando apenas origens de tráfego ligadas à campanha quando os códigos
   * existem; antes da campanha existir, usa a versão do slot publicado no destino do experimento.
   */
  private PdeMembershipMetric aggregatePdeMembershipMetric(
      PdeAnalyticsSummary summary,
      List<String> attributionCodes,
      String followUpActionUrl,
      boolean allowVersionFallback) {
    if (attributionCodes == null
        || attributionCodes.isEmpty()
        || summary.trafficSources() == null) {
      if (!allowVersionFallback) {
        return PdeMembershipMetric.empty();
      }
      return aggregatePdeMembershipMetricByExperienceVersion(summary, followUpActionUrl);
    }
    long pdeEntries = 0;
    long videoPartial = 0;
    long videoComplete = 0;
    long loginStarted = 0;
    long paywallViewed = 0;
    long checkoutIntent = 0;
    long subscriptionApproved = 0;
    Instant lastEventAt = null;
    boolean matchedAttribution = false;
    for (PdeAnalyticsSummary.PdeTrafficSourceMetric source : summary.trafficSources()) {
      if (source == null || !matchesPdeAttribution(source, attributionCodes)) {
        continue;
      }
      matchedAttribution = true;
      pdeEntries += source.pdeEntries();
      videoPartial += source.videoPartial();
      videoComplete += source.videoComplete();
      loginStarted += source.loginStarted();
      paywallViewed += source.paywallViewed();
      checkoutIntent += source.checkoutStarted();
      subscriptionApproved += source.subscriptionApproved();
      lastEventAt = max(lastEventAt, parsePdeInstant(source.lastEventAt()));
    }
    if (!matchedAttribution
        && allowVersionFallback
        && findMatchingExperienceVersion(summary, followUpActionUrl).isPresent()) {
      log.warn(
          "Analytics PDE sem origem UTM correspondente; usando fallback pela versao do slot. followUpActionUrl={} attributionCodes={}",
          followUpActionUrl,
          attributionCodes);
      return aggregatePdeMembershipMetricByExperienceVersion(summary, followUpActionUrl);
    }
    return new PdeMembershipMetric(
        pdeEntries,
        videoPartial,
        videoComplete,
        loginStarted,
        paywallViewed,
        checkoutIntent,
        subscriptionApproved,
        summary.accessReleased(),
        summary.firstUse(),
        lastEventAt);
  }

  /**
   * Agrega o funil PDE pela versão comercial quando ainda não há UTMs/campanha gravadas para
   * atribuição.
   */
  private PdeMembershipMetric aggregatePdeMembershipMetricByExperienceVersion(
      PdeAnalyticsSummary summary, String followUpActionUrl) {
    Optional<PdeAnalyticsSummary.PdeExperienceVersionMetric> matchingVersion =
        findMatchingExperienceVersion(summary, followUpActionUrl);
    if (matchingVersion.isPresent()) {
      PdeAnalyticsSummary.PdeExperienceVersionMetric version = matchingVersion.get();
      return new PdeMembershipMetric(
          version.pdeEntries(),
          version.videoPartial(),
          version.videoComplete(),
          version.loginStarted(),
          version.paywallViewed(),
          Math.max(version.subscriptionClicked(), version.checkoutStarted()),
          version.subscriptionApproved(),
          summary.accessReleased(),
          summary.firstUse(),
          parsePdeInstant(summary.lastEventAt()));
    }
    return new PdeMembershipMetric(
        summary.pedEntries(),
        sumPdeEventTypes(summary, "VIDEO_PROGRESS_25", "VIDEO_PROGRESS_50", "VIDEO_PROGRESS_75"),
        sumPdeEventTypes(summary, "VIDEO_COMPLETED"),
        summary.loginStarted(),
        summary.paywallViewed(),
        Math.max(summary.subscriptionClicked(), summary.checkoutStarted()),
        summary.subscriptionApproved(),
        summary.accessReleased(),
        summary.firstUse(),
        parsePdeInstant(summary.lastEventAt()));
  }

  /** Monta o diagnóstico com a mesma decisão de atribuição e fallback usada no funil PDE. */
  private ExperimentPdeCockpitDiagnosticsDto buildPdeCockpitDiagnostics(
      Long experimentId,
      String followUpActionUrl,
      String normalizedDomain,
      String versionToken,
      ExpectedPdeExperienceVersion expectedVersion,
      PdeAnalyticsSummary summary,
      List<String> attributionCodes) {
    List<String> safeAttributionCodes =
        attributionCodes == null ? List.of() : attributionCodes.stream().distinct().toList();
    Optional<PdeAnalyticsSummary.PdeExperienceVersionMetric> matchingVersion =
        findMatchingExperienceVersion(summary, followUpActionUrl);
    boolean attributionFilterApplied =
        !safeAttributionCodes.isEmpty() && summary.trafficSources() != null;
    List<PdeAnalyticsSummary.PdeTrafficSourceMetric> matchingTrafficSources =
        attributionFilterApplied
            ? summary.trafficSources().stream()
                .filter(
                    source -> source != null && matchesPdeAttribution(source, safeAttributionCodes))
                .toList()
            : List.of();
    boolean fallbackUsed =
        shouldUsePdeDiagnosticFallback(
            attributionFilterApplied, matchingTrafficSources, matchingVersion);
    return new ExperimentPdeCockpitDiagnosticsDto(
        experimentId,
        true,
        followUpActionUrl,
        normalizedDomain,
        followUpActionUrl,
        DEFAULT_PDE_PRODUCT_SLUG,
        true,
        summary.currentExperienceVersion(),
        expectedVersion.value().orElse(null),
        expectedVersion.source(),
        versionToken,
        matchingVersion
            .map(PdeAnalyticsSummary.PdeExperienceVersionMetric::experienceVersion)
            .orElse(null),
        attributionFilterApplied,
        safeAttributionCodes,
        matchingTrafficSources.size(),
        matchingTrafficSources.stream()
            .mapToLong(PdeAnalyticsSummary.PdeTrafficSourceMetric::sessions)
            .sum(),
        fallbackUsed,
        resolvePdeDiagnosticFallbackReason(
            attributionFilterApplied, matchingTrafficSources, matchingVersion),
        toPdeExperienceVersionDiagnostics(summary),
        null,
        null);
  }

  /** Indica se o diagnóstico deve marcar que o cockpit precisou fugir do filtro por UTM. */
  private boolean shouldUsePdeDiagnosticFallback(
      boolean attributionFilterApplied,
      List<PdeAnalyticsSummary.PdeTrafficSourceMetric> matchingTrafficSources,
      Optional<PdeAnalyticsSummary.PdeExperienceVersionMetric> matchingVersion) {
    if (!attributionFilterApplied) {
      return true;
    }
    return matchingTrafficSources.isEmpty() && matchingVersion.isPresent();
  }

  /** Explica o motivo do fallback para facilitar investigação do cockpit zerado. */
  private String resolvePdeDiagnosticFallbackReason(
      boolean attributionFilterApplied,
      List<PdeAnalyticsSummary.PdeTrafficSourceMetric> matchingTrafficSources,
      Optional<PdeAnalyticsSummary.PdeExperienceVersionMetric> matchingVersion) {
    if (!attributionFilterApplied && matchingVersion.isPresent()) {
      return "NO_ATTRIBUTION_CODES_VERSION_MATCH";
    }
    if (!attributionFilterApplied) {
      return "NO_ATTRIBUTION_CODES_GLOBAL_SUMMARY";
    }
    if (matchingTrafficSources.isEmpty() && matchingVersion.isPresent()) {
      return "ATTRIBUTION_NOT_MATCHING_VERSION_AVAILABLE";
    }
    if (matchingTrafficSources.isEmpty()) {
      return "ATTRIBUTION_NOT_MATCHING_NO_VERSION";
    }
    return "ATTRIBUTION_MATCHED";
  }

  /** Lista as versões PDE retornadas pelo summary para comparar com a versão esperada. */
  private List<PdeExperienceVersionDiagnosticDto> toPdeExperienceVersionDiagnostics(
      PdeAnalyticsSummary summary) {
    if (summary.experienceVersions() == null) {
      return List.of();
    }
    return summary.experienceVersions().stream()
        .filter(version -> version != null)
        .map(
            version ->
                new PdeExperienceVersionDiagnosticDto(
                    version.experienceVersion(),
                    version.totalEvents(),
                    version.sessions(),
                    version.pdeEntries(),
                    version.videoPartial(),
                    version.videoComplete(),
                    version.loginStarted(),
                    version.paywallViewed(),
                    Math.max(version.subscriptionClicked(), version.checkoutStarted()),
                    version.subscriptionApproved()))
        .toList();
  }

  /** Localiza a versão comercial do PDE correspondente ao slot versionado da URL do experimento. */
  private Optional<PdeAnalyticsSummary.PdeExperienceVersionMetric> findMatchingExperienceVersion(
      PdeAnalyticsSummary summary, String followUpActionUrl) {
    if (summary.experienceVersions() == null || followUpActionUrl == null) {
      return Optional.empty();
    }
    Optional<String> expectedExperienceVersion =
        resolveExpectedPdeExperienceVersion(followUpActionUrl);
    if (expectedExperienceVersion.isPresent()) {
      return summary.experienceVersions().stream()
          .filter(metric -> metric != null && metric.experienceVersion() != null)
          .filter(metric -> metric.experienceVersion().equals(expectedExperienceVersion.get()))
          .findFirst();
    }
    Optional<String> versionToken = resolveVersionTokenFromUrl(followUpActionUrl);
    if (versionToken.isEmpty()) {
      return Optional.empty();
    }
    return summary.experienceVersions().stream()
        .filter(metric -> metric != null && metric.experienceVersion() != null)
        .filter(metric -> metric.experienceVersion().contains(versionToken.get()))
        .findFirst();
  }

  /**
   * Resolve a versão comercial esperada pelo cadastro operacional do slot publicado no Marketing
   * Hub.
   */
  private Optional<String> resolveExpectedPdeExperienceVersion(String followUpActionUrl) {
    return resolveExpectedPdeExperienceVersionDiagnostic(followUpActionUrl).value();
  }

  /** Resolve a versão esperada do PDE e registra a origem dessa decisão para diagnóstico. */
  private ExpectedPdeExperienceVersion resolveExpectedPdeExperienceVersionDiagnostic(
      String followUpActionUrl) {
    return normalizeDomainFromUrl(followUpActionUrl)
        .flatMap(pdeProductionSlotRepository::findFirstByDomain)
        .map(this::toExpectedPdeExperienceVersion)
        .orElse(new ExpectedPdeExperienceVersion(Optional.empty(), "NONE"));
  }

  /** Extrai a versão comercial do slot PDE quando o cadastro operacional a publicou. */
  private ExpectedPdeExperienceVersion toExpectedPdeExperienceVersion(PdeProductionSlot slot) {
    String experienceVersion = slot.getExperienceVersion();
    if (experienceVersion == null || experienceVersion.isBlank()) {
      return new ExpectedPdeExperienceVersion(Optional.empty(), "SLOT_WITHOUT_EXPERIENCE_VERSION");
    }
    return new ExpectedPdeExperienceVersion(Optional.of(experienceVersion), "PDE_PRODUCTION_SLOT");
  }

  /** Extrai o token de versão da URL pública quando o slot ainda não existe no cadastro. */
  private Optional<String> resolveVersionTokenFromUrl(String followUpActionUrl) {
    if (followUpActionUrl == null || followUpActionUrl.isBlank()) {
      return Optional.empty();
    }
    Matcher matcher = MUSA_VERSIONED_HOST_PATTERN.matcher(followUpActionUrl.trim());
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of("-v" + matcher.group(1) + "-");
  }

  /** Normaliza o domínio de uma URL para consulta do slot produtivo PDE. */
  private Optional<String> normalizeDomainFromUrl(String url) {
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    String normalized = url.trim().replaceFirst("^https?://", "").replaceFirst("^//", "");
    int pathStart = normalized.indexOf('/');
    if (pathStart >= 0) {
      normalized = normalized.substring(0, pathStart);
    }
    int queryStart = normalized.indexOf('?');
    if (queryStart >= 0) {
      normalized = normalized.substring(0, queryStart);
    }
    int hashStart = normalized.indexOf('#');
    if (hashStart >= 0) {
      normalized = normalized.substring(0, hashStart);
    }
    int portStart = normalized.indexOf(':');
    if (portStart >= 0) {
      normalized = normalized.substring(0, portStart);
    }
    if (normalized.isBlank() || !normalized.contains(".")) {
      return Optional.empty();
    }
    return Optional.of(normalized.toLowerCase());
  }

  /** Soma eventos PDE globais preservando compatibilidade com contratos antigos do backend PDE. */
  private long sumPdeEventTypes(PdeAnalyticsSummary summary, String... eventTypes) {
    if (summary.events() == null || eventTypes == null || eventTypes.length == 0) {
      return 0;
    }
    List<String> expected = Arrays.asList(eventTypes);
    return summary.events().stream()
        .filter(metric -> metric != null && expected.contains(metric.eventType()))
        .mapToLong(PdeAnalyticsSummary.PdeEventMetric::total)
        .sum();
  }

  /** Confirma se a origem PDE pertence ao experimento por campanha ou criativo Meta. */
  private boolean matchesPdeAttribution(
      PdeAnalyticsSummary.PdeTrafficSourceMetric source, List<String> attributionCodes) {
    return attributionCodes.contains(source.utmCampaign())
        || attributionCodes.contains(source.utmContent());
  }

  /**
   * Converte datas ISO do PDE em Instant, ignorando valores ausentes ou inválidos sem quebrar o
   * painel.
   */
  private Instant parsePdeInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value.trim());
    } catch (DateTimeParseException ex) {
      try {
        return OffsetDateTime.parse(value.trim()).toInstant();
      } catch (DateTimeParseException nestedEx) {
        log.warn("Data de último evento PDE ignorada por formato inválido; value={}", value);
        return null;
      }
    }
  }

  /** Mescla uma métrica automática na etapa do funil correspondente. */
  private void mergeMetric(
      Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages,
      ExperimentFunnelStage stage,
      AggregatedMetric metric,
      String source) {
    if (metric == null) {
      return;
    }
    ExperimentFunnelStageDto dto = stages.get(stage);
    if (dto == null) {
      return;
    }
    dto.setAutoCount(metric.total());
    dto.setTotalCount(dto.getManualCount() + metric.total());
    dto.setUniqueCount(sum(dto.getUniqueCount(), metric.uniqueCount()));
    dto.setLastEventAt(max(dto.getLastEventAt(), metric.lastEvent()));
    dto.setSource(source);
  }

  /**
   * Adapta nomes e fontes do funil quando o experimento é venda direta low-ticket, sem alterar
   * chaves históricas.
   */
  private void adaptStagesForExperimentType(
      Experiment experiment, Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
    if (isPdeMembershipSubscriptionFunnel(experiment)) {
      adaptStagesForPdeMembershipSubscription(stages);
      return;
    }
    if (!isLowTicketProduct(experiment)) {
      return;
    }
    renameStage(
        stages,
        ExperimentFunnelStage.ACESSO_FORM_LEAD,
        "Clique para a página de venda",
        "Cliques do anúncio para a página de venda (experiment_campaign_metric)");
    renameStage(
        stages,
        ExperimentFunnelStage.VISUALIZACAO_FORM,
        "Visualização da página de venda",
        "Visualizações da página de venda publicadas pelo GeraSalesPage (page_view)");
    renameStage(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_PARCIAL,
        "Vídeo da página visto parcial",
        "Sinal de consumo parcial do vídeo da página de venda");
    renameStage(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_COMPLETO,
        "Vídeo da página visto completo",
        "Sinal de consumo completo do vídeo da página de venda");
    renameStage(
        stages,
        ExperimentFunnelStage.ACESSO_CHECKOUT,
        "Clique no checkout",
        "Cliques reais no checkout da página de venda (checkout_click)");
  }

  /**
   * Adapta o funil para produtos PDE com login, assinatura e ativação dentro da área de membros.
   */
  private void adaptStagesForPdeMembershipSubscription(
      Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
    renameStage(
        stages,
        ExperimentFunnelStage.ACESSO_FORM_LEAD,
        "Clique no anúncio para o PED/MUSA",
        "Cliques do anúncio para a tela inicial do PED/MUSA (experiment_campaign_metric)");
    renameStage(
        stages,
        ExperimentFunnelStage.VISUALIZACAO_FORM,
        "Entrada na tela inicial do PED/MUSA",
        "Visualizações da tela inicial do PED/MUSA publicadas pelo GeraSalesPage ou PDE (page_view)");
    renameStage(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_PARCIAL,
        "Vídeo do PDE/MUSA visto parcial",
        "Sinal de consumo parcial do vídeo explicativo dentro do PED/MUSA");
    renameStage(
        stages,
        ExperimentFunnelStage.VIDEO_VISTO_COMPLETO,
        "Vídeo do PDE/MUSA visto completo",
        "Sinal de consumo completo do vídeo explicativo dentro do PED/MUSA");
    renameStage(
        stages,
        ExperimentFunnelStage.ENVIO_FORM,
        "Login ou criação de conta",
        "Entradas identificadas na área PDE/MUSA por login ou criação de conta");
    renameStage(
        stages,
        ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA,
        "Visualização da oferta de assinatura",
        "Visualizações da oferta ou paywall de assinatura dentro do PED/MUSA");
    renameStage(
        stages,
        ExperimentFunnelStage.ACESSO_CHECKOUT,
        "Clique no plano/checkout",
        "Cliques reais em plano ou checkout de assinatura");
    renameStage(
        stages,
        ExperimentFunnelStage.COMPRA,
        "Assinatura aprovada",
        "Assinaturas aprovadas pelo checkout/webhook");
    renameStage(
        stages,
        ExperimentFunnelStage.ABERTURA_EMAIL_COMPRA,
        "Acesso liberado",
        "Liberações de acesso após assinatura aprovada");
    renameStage(
        stages,
        ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO,
        "Primeiro uso/ativação",
        "Primeiro uso real da área: missão, diagnóstico ou material consumido");
  }

  /** Troca label e fonte da etapa quando a métrica já foi consolidada para outro tipo comercial. */
  private void renameStage(
      Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages,
      ExperimentFunnelStage stage,
      String label,
      String source) {
    ExperimentFunnelStageDto dto = stages.get(stage);
    if (dto == null) {
      return;
    }
    dto.setLabel(label);
    dto.setSource(source);
  }

  /** Decide quais etapas aparecem para o tipo comercial do experimento. */
  private boolean shouldExposeStage(Experiment experiment, ExperimentFunnelStage stage) {
    if (isPdeMembershipSubscriptionFunnel(experiment)) {
      return stage == ExperimentFunnelStage.VISUALIZACAO_ANUNCIO
          || stage == ExperimentFunnelStage.ACESSO_FORM_LEAD
          || stage == ExperimentFunnelStage.VISUALIZACAO_FORM
          || stage == ExperimentFunnelStage.VIDEO_VISTO_PARCIAL
          || stage == ExperimentFunnelStage.VIDEO_VISTO_COMPLETO
          || stage == ExperimentFunnelStage.ENVIO_FORM
          || stage == ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA
          || stage == ExperimentFunnelStage.ACESSO_CHECKOUT
          || stage == ExperimentFunnelStage.COMPRA
          || stage == ExperimentFunnelStage.ABERTURA_EMAIL_COMPRA
          || stage == ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO;
    }
    if (!isLowTicketProduct(experiment)) {
      return true;
    }
    return stage == ExperimentFunnelStage.VISUALIZACAO_ANUNCIO
        || stage == ExperimentFunnelStage.ACESSO_FORM_LEAD
        || stage == ExperimentFunnelStage.VISUALIZACAO_FORM
        || stage == ExperimentFunnelStage.VIDEO_VISTO_PARCIAL
        || stage == ExperimentFunnelStage.VIDEO_VISTO_COMPLETO
        || stage == ExperimentFunnelStage.ACESSO_CHECKOUT
        || stage == ExperimentFunnelStage.COMPRA
        || stage == ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO;
  }

  /** Identifica se o experimento segue o fluxo comercial de venda direta low-ticket. */
  private boolean isLowTicketProduct(Experiment experiment) {
    return experiment != null
        && experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT;
  }

  /** Identifica se o experimento mede assinatura e ativação de um produto PDE. */
  private boolean isPdeMembershipSubscriptionFunnel(Experiment experiment) {
    return experiment != null
        && (experiment.getExperimentType() == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL
            || isFakePdeExperiment(experiment));
  }

  /** Identifica funis em que clique de checkout é evento comercial central. */
  private boolean isPurchaseIntentFunnel(Experiment experiment) {
    return isLowTicketProduct(experiment) || isPdeMembershipSubscriptionFunnel(experiment);
  }

  /** Identifica experimento fake usado para testar uma versão publicada do PDE/MUSA. */
  private boolean isFakePdeExperiment(Experiment experiment) {
    return isFakeExperiment(experiment)
        && resolveVersionTokenFromUrl(experiment.getFollowUpActionUrl()).isPresent();
  }

  /** Identifica se o experimento é operacional e não deve receber fallback comercial global. */
  private boolean isFakeExperiment(Experiment experiment) {
    return experiment != null && experiment.getExperimentType() == ExperimentType.FAKE_EXPERIMENT;
  }

  /** Executa consulta agregada única e converte o resultado para métrica interna. */
  private AggregatedMetric fetchSingleMetric(String sql, Object... args) {
    return jdbcTemplate.query(
        sql,
        rs -> {
          if (!rs.next()) {
            return null;
          }
          long total = rs.getLong("total");
          Long unique = (Long) rs.getObject("unique_count");
          LocalDateTime databaseDateTime = rs.getObject("last_event", LocalDateTime.class);
          Instant last = fromUtcDatabaseDateTime(databaseDateTime);
          return new AggregatedMetric(total, unique, last);
        },
        args);
  }

  /**
   * Resolve o marco temporal usado para ignorar eventos anteriores a publicação ou reset do funil.
   */
  private Instant resolveBaseline(Experiment experiment) {
    if (experiment == null) {
      return null;
    }
    return max(experiment.getFacebookReleaseRequestedAt(), experiment.getFunnelResetAt());
  }

  /** Converte o marco UTC para o DATETIME canônico usado nas consultas JDBC do funil. */
  static LocalDateTime toUtcDatabaseDateTime(Instant baseline) {
    return baseline == null ? null : LocalDateTime.ofInstant(baseline, ZoneOffset.UTC);
  }

  /** Converte o DATETIME UTC canônico do banco sem aplicar novamente o fuso da JVM. */
  static Instant fromUtcDatabaseDateTime(LocalDateTime value) {
    return value == null ? null : value.toInstant(ZoneOffset.UTC);
  }

  /** Converte tipos temporais de projeções nativas para o instante UTC canônico do analytics. */
  static Instant fromUtcDatabaseValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof LocalDateTime localDateTime) {
      return fromUtcDatabaseDateTime(localDateTime);
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    if (value instanceof java.sql.Timestamp timestamp) {
      return fromUtcDatabaseDateTime(timestamp.toLocalDateTime());
    }
    throw new IllegalArgumentException(
        "Tipo temporal de analytics não suportado: " + value.getClass().getName());
  }

  /** Normaliza o código de campanha para o tamanho aceito pelo banco. */
  private String normalizeCampaignCode(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.length() > MAX_CAMPAIGN_CODE_LENGTH
        ? trimmed.substring(0, MAX_CAMPAIGN_CODE_LENGTH)
        : trimmed;
  }

  /** Preenche a origem padrão em etapas que possuem apenas eventos manuais. */
  private void fillDefaultSourceWhenMissing(
      Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
    stages
        .values()
        .forEach(
            dto -> {
              if (dto.getSource() == null && dto.getManualCount() > 0) {
                dto.setSource("Eventos manuais registrados na aplicação");
              }
            });
  }

  /** Soma contadores opcionais preservando null quando ambos estão ausentes. */
  private Long sum(Long a, Long b) {
    if (a == null && b == null) return null;
    return Optional.ofNullable(a).orElse(0L) + Optional.ofNullable(b).orElse(0L);
  }

  /** Retorna o instante mais recente entre dois valores opcionais. */
  private Instant max(Instant a, Instant b) {
    if (a == null) return b;
    if (b == null) return a;
    return a.isAfter(b) ? a : b;
  }

  /** Normaliza valores do payload textual para preservar o delimitador operacional entre campos. */
  private String sanitizePayloadValue(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().replace(";", ",").replace("\r", " ").replace("\n", " ");
  }

  /** Consolida a distribuição percentual de sessões por tipo de dispositivo. */
  private List<ExperimentLandingAnalyticsDeviceDto> buildDeviceBreakdown(
      Map<String, LandingAnalyticsSessionAccumulator> sessions) {
    long totalSessions = sessions.size();
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("mobile", 0L);
    counts.put("desktop", 0L);
    counts.put("tablet", 0L);
    sessions
        .values()
        .forEach(
            session ->
                counts.compute(
                    session.deviceType(), (key, count) -> (count == null ? 0 : count) + 1));
    return counts.entrySet().stream()
        .filter(
            entry ->
                "mobile".equals(entry.getKey())
                    || "desktop".equals(entry.getKey())
                    || "tablet".equals(entry.getKey()))
        .map(
            entry ->
                new ExperimentLandingAnalyticsDeviceDto(
                    entry.getKey(),
                    landingAnalyticsDeviceLabel(entry.getKey()),
                    entry.getValue(),
                    totalSessions == 0
                        ? 0
                        : Math.round((entry.getValue() * 10000.0) / totalSessions) / 100.0))
        .toList();
  }

  /** Consolida a distribuição percentual de sessões mobile por sistema operacional. */
  private List<ExperimentLandingAnalyticsOperatingSystemDto> buildMobileOperatingSystemBreakdown(
      Map<String, LandingAnalyticsSessionAccumulator> sessions) {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("ios", 0L);
    counts.put("android", 0L);
    counts.put("other", 0L);
    sessions.values().stream()
        .filter(session -> "mobile".equals(session.deviceType()))
        .forEach(
            session ->
                counts.compute(
                    session.operatingSystem(), (key, count) -> (count == null ? 0 : count) + 1));
    long mobileSessions = counts.values().stream().mapToLong(Long::longValue).sum();
    return counts.entrySet().stream()
        .map(
            entry ->
                new ExperimentLandingAnalyticsOperatingSystemDto(
                    entry.getKey(),
                    landingAnalyticsOperatingSystemLabel(entry.getKey()),
                    entry.getValue(),
                    mobileSessions == 0
                        ? 0
                        : Math.round((entry.getValue() * 10000.0) / mobileSessions) / 100.0))
        .toList();
  }

  /** Consolida a distribuição percentual das principais resoluções de tela por sessão. */
  private List<ExperimentLandingAnalyticsScreenSizeDto> buildScreenSizeBreakdown(
      Map<String, LandingAnalyticsSessionAccumulator> sessions) {
    long totalSessionsWithScreen =
        sessions.values().stream()
            .filter(LandingAnalyticsSessionAccumulator::hasScreenSize)
            .count();
    Map<String, ScreenSizeAccumulator> counts = new LinkedHashMap<>();
    sessions.values().stream()
        .filter(LandingAnalyticsSessionAccumulator::hasScreenSize)
        .forEach(
            session ->
                counts
                    .computeIfAbsent(
                        session.screenSizeKey(),
                        key ->
                            new ScreenSizeAccumulator(
                                session.screenWidth(), session.screenHeight()))
                    .record());
    return counts.values().stream()
        .sorted(Comparator.comparingLong(ScreenSizeAccumulator::sessions).reversed())
        .limit(8)
        .map(
            screen ->
                new ExperimentLandingAnalyticsScreenSizeDto(
                    screen.key(),
                    screen.label(),
                    screen.width(),
                    screen.height(),
                    screen.sessions(),
                    totalSessionsWithScreen == 0
                        ? 0
                        : Math.round((screen.sessions() * 10000.0) / totalSessionsWithScreen)
                            / 100.0))
        .toList();
  }

  /** Consolida métricas técnicas de carregamento enviadas pelos eventos page_load_metric. */
  private ExperimentLandingAnalyticsLoadMetricDto buildLoadMetrics(
      List<LandingAnalyticsEventRow> rows,
      Map<String, LandingAnalyticsSessionAccumulator> sessions) {
    List<Long> loadDurations = new ArrayList<>();
    List<Long> domContentLoadedDurations = new ArrayList<>();
    List<Long> firstContentfulPaintDurations = new ArrayList<>();
    long totalResourceErrors = 0;
    for (LandingAnalyticsEventRow row : rows) {
      Map<String, String> payload = parseDelimitedPayload(row.payload());
      String eventType = firstNonBlank(payload.get("eventType"), "desconhecido");
      if (!"page_load_metric".equalsIgnoreCase(eventType)) {
        continue;
      }
      String sessionId = firstNonBlank(payload.get("sessionId"), "sem-sessao");
      if (!sessions.containsKey(sessionId)) {
        continue;
      }
      addPositive(loadDurations, parseLong(payload.get("loadDurationMs")));
      addPositive(domContentLoadedDurations, parseLong(payload.get("domContentLoadedMs")));
      addPositive(firstContentfulPaintDurations, parseLong(payload.get("firstContentfulPaintMs")));
      totalResourceErrors += Math.max(0, parseLong(payload.get("resourceErrorCount")));
    }
    long averageLoadDurationMs = average(loadDurations);
    long p95LoadDurationMs = percentile95(loadDurations);
    long sessionsWithoutSectionEvents =
        sessions.values().stream().filter(session -> session.sectionViewEvents() == 0).count();
    double initialEngagementRate =
        sessions.isEmpty()
            ? 0
            : Math.round(
                    ((sessions.size() - sessionsWithoutSectionEvents) * 10000.0) / sessions.size())
                / 100.0;
    long inAppBrowserSessions =
        sessions.values().stream()
            .filter(session -> isInAppBrowserUserAgent(session.lastUserAgent()))
            .count();
    double inAppBrowserPercentage =
        sessions.isEmpty()
            ? 0
            : Math.round((inAppBrowserSessions * 10000.0) / sessions.size()) / 100.0;
    LoadHealthDiagnosis diagnosis =
        diagnoseLoadHealth(
            loadDurations.size(),
            averageLoadDurationMs,
            p95LoadDurationMs,
            totalResourceErrors,
            sessions.size(),
            initialEngagementRate,
            inAppBrowserPercentage);
    return new ExperimentLandingAnalyticsLoadMetricDto(
        loadDurations.size(),
        averageLoadDurationMs,
        p95LoadDurationMs,
        average(domContentLoadedDurations),
        average(firstContentfulPaintDurations),
        totalResourceErrors,
        sessionsWithoutSectionEvents,
        initialEngagementRate,
        inAppBrowserSessions,
        inAppBrowserPercentage,
        diagnosis.code(),
        diagnosis.label(),
        diagnosis.severity(),
        diagnosis.summary(),
        diagnosis.recommendation());
  }

  /**
   * Classifica a saúde técnica da landing cruzando carregamento, engajamento inicial e navegador
   * in-app.
   */
  private LoadHealthDiagnosis diagnoseLoadHealth(
      long loadEvents,
      long averageLoadDurationMs,
      long p95LoadDurationMs,
      long totalResourceErrors,
      long totalSessions,
      double initialEngagementRate,
      double inAppBrowserPercentage) {
    if (loadEvents == 0) {
      return new LoadHealthDiagnosis(
          "INSUFFICIENT_DATA",
          "Dados insuficientes",
          "info",
          "Ainda não há eventos técnicos de carregamento suficientes para diagnosticar a landing.",
          "Aguarde novos acessos reais após a publicação ou reabra a landing para gerar page_load_metric.");
    }
    if (p95LoadDurationMs >= 8000 || averageLoadDurationMs >= 5000) {
      return new LoadHealthDiagnosis(
          "CRITICAL_SLOW_LOAD",
          "Carregamento crítico",
          "danger",
          "Parte relevante dos visitantes está recebendo a página com lentidão crítica.",
          "Priorize reduzir peso da primeira dobra, imagens e scripts antes de escalar tráfego pago.");
    }
    if (totalResourceErrors > 0) {
      return new LoadHealthDiagnosis(
          "RESOURCE_ERRORS",
          "Falhas de recursos",
          "danger",
          "O navegador reportou falhas ao carregar imagens, scripts ou estilos da landing.",
          "Verifique URLs de imagens, assets externos e disponibilidade do domínio da landing.");
    }
    if (p95LoadDurationMs >= 4000 || averageLoadDurationMs >= 2500) {
      return new LoadHealthDiagnosis(
          "SLOW_LOAD",
          "Carregamento lento",
          "warning",
          "A landing carrega, mas o tempo técnico já pode reduzir engajamento inicial.",
          "Otimize imagens e scripts e compare o próximo ciclo antes de aumentar investimento.");
    }
    if (inAppBrowserPercentage >= 60 && initialEngagementRate < 50) {
      return new LoadHealthDiagnosis(
          "POSSIBLE_IN_APP_BROWSER",
          "Atenção no navegador do app",
          "warning",
          "A maior parte das sessões vem de navegador in-app e o engajamento inicial está baixo.",
          "Compare Instagram/Facebook in-app com navegador externo e revise peso visual no mobile.");
    }
    if (totalSessions >= 5 && initialEngagementRate < 25) {
      return new LoadHealthDiagnosis(
          "POSSIBLE_TRAFFIC_QUALITY",
          "Possível baixa qualidade de tráfego",
          "warning",
          "O carregamento não parece ser o principal gargalo, mas poucos visitantes veem seções da landing.",
          "Revise promessa do anúncio, segmentação e correspondência entre criativo e primeira dobra.");
    }
    return new LoadHealthDiagnosis(
        "GOOD",
        "Carregamento saudável",
        "success",
        "Os sinais técnicos de carregamento não indicam gargalo relevante neste momento.",
        "Continue acompanhando P95 e engajamento inicial ao aumentar volume de tráfego.");
  }

  /**
   * Identifica user agents de navegadores internos de apps sociais que podem afetar carregamento e
   * medição.
   */
  private boolean isInAppBrowserUserAgent(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return false;
    }
    String normalized = userAgent.toLowerCase();
    return normalized.contains("instagram")
        || normalized.contains("fban")
        || normalized.contains("fbav")
        || normalized.contains("iabmv");
  }

  /** Adiciona valor positivo à lista de durações consolidadas. */
  private void addPositive(List<Long> values, long value) {
    if (value > 0) {
      values.add(value);
    }
  }

  /** Calcula média inteira de uma lista de durações em milissegundos. */
  private long average(List<Long> values) {
    if (values.isEmpty()) {
      return 0;
    }
    return Math.round(values.stream().mapToLong(Long::longValue).average().orElse(0));
  }

  /** Calcula percentil 95 simples para identificar piora percebida por parte dos visitantes. */
  private long percentile95(List<Long> values) {
    if (values.isEmpty()) {
      return 0;
    }
    List<Long> sorted = values.stream().sorted().toList();
    int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
    return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
  }

  /** Normaliza o tipo de dispositivo enviado pela landing, usando user-agent como fallback. */
  private static String normalizeLandingAnalyticsDeviceType(
      String rawDeviceType, String userAgent) {
    if (rawDeviceType != null && !rawDeviceType.isBlank()) {
      String normalized = rawDeviceType.trim().toLowerCase();
      if ("mobile".equals(normalized)
          || "tablet".equals(normalized)
          || "desktop".equals(normalized)) {
        return normalized;
      }
      if ("computador".equals(normalized) || "computer".equals(normalized)) {
        return "desktop";
      }
    }
    String normalizedUserAgent = userAgent == null ? "" : userAgent.toLowerCase();
    if (normalizedUserAgent.contains("ipad")
        || normalizedUserAgent.contains("tablet")
        || (normalizedUserAgent.contains("android") && !normalizedUserAgent.contains("mobile"))) {
      return "tablet";
    }
    if (normalizedUserAgent.contains("mobi")
        || normalizedUserAgent.contains("iphone")
        || normalizedUserAgent.contains("ipod")
        || normalizedUserAgent.contains("android")) {
      return "mobile";
    }
    return "desktop";
  }

  /**
   * Normaliza o sistema operacional mobile enviado pela landing, usando user-agent como fallback.
   */
  private static String normalizeLandingAnalyticsOperatingSystem(
      String rawOperatingSystem, String userAgent) {
    if (rawOperatingSystem != null && !rawOperatingSystem.isBlank()) {
      String normalized = rawOperatingSystem.trim().toLowerCase();
      if ("ios".equals(normalized) || "iphone".equals(normalized) || "ipad".equals(normalized)) {
        return "ios";
      }
      if ("android".equals(normalized)) {
        return "android";
      }
    }
    String normalizedUserAgent = userAgent == null ? "" : userAgent.toLowerCase();
    if (normalizedUserAgent.contains("iphone")
        || normalizedUserAgent.contains("ipod")
        || normalizedUserAgent.contains("ipad")) {
      return "ios";
    }
    if (normalizedUserAgent.contains("android")) {
      return "android";
    }
    return "other";
  }

  /** Retorna o rótulo do sistema operacional mobile exibido no painel de analytics. */
  private static String landingAnalyticsOperatingSystemLabel(String operatingSystem) {
    return switch (normalizeLandingAnalyticsOperatingSystem(operatingSystem, null)) {
      case "ios" -> "iOS";
      case "android" -> "Android";
      default -> "Outros";
    };
  }

  /** Retorna o rótulo do dispositivo exibido no painel de analytics. */
  private static String landingAnalyticsDeviceLabel(String deviceType) {
    return switch (normalizeLandingAnalyticsDeviceType(deviceType, null)) {
      case "mobile" -> "Mobile";
      case "tablet" -> "Tablet";
      default -> "Computador";
    };
  }

  /**
   * Busca no repositório centralizado os eventos de analytics da landing para o marco temporal
   * atual.
   */
  private List<LandingAnalyticsEventRow> fetchLandingAnalyticsEvents(
      Long experimentId, Instant baseline) {
    return eventRepository
        .findLandingAnalyticsEvents(
            experimentId,
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            baseline,
            PageRequest.of(0, 2000))
        .stream()
        .map(
            event ->
                new LandingAnalyticsEventRow(
                    event.getId(), event.getPayload(), event.getOccurredAt()))
        .toList();
  }

  /**
   * Converte o payload textual em pares chave/valor sem depender de JSON serializado em campo
   * textual.
   */
  private Map<String, String> parseDelimitedPayload(String payload) {
    if (payload == null || payload.isBlank()) {
      return Map.of();
    }
    Map<String, String> result = new HashMap<>();
    for (String token : payload.split(";")) {
      int separator = token.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      String key = token.substring(0, separator).trim();
      String value = token.substring(separator + 1).trim();
      if (!key.isEmpty()) {
        result.put(key, value);
      }
    }
    return result;
  }

  /** Retorna o primeiro valor textual preenchido ou o fallback operacional. */
  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  /** Representa a versão esperada do PDE e a origem operacional dessa decisão. */
  private record ExpectedPdeExperienceVersion(Optional<String> value, String source) {}

  /**
   * Converte números textuais de duração para long, retornando zero quando ausentes ou inválidos.
   */
  private long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      log.warn("Landing analytics elapsedMs inválido. elapsedMs={}", value, ex);
      return 0;
    }
  }

  /**
   * Converte números textuais positivos de tela para Integer, retornando null quando ausentes ou
   * inválidos.
   */
  private Integer parsePositiveInteger(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : null;
    } catch (NumberFormatException ex) {
      log.warn("Landing analytics dimensão de tela inválida. valor={}", value, ex);
      return null;
    }
  }

  /**
   * Representa a classificação operacional de saúde de carregamento exibida na tela de analytics.
   */
  private record LoadHealthDiagnosis(
      String code, String label, String severity, String summary, String recommendation) {}

  /** Linha mínima de evento de analytics retornada da tabela de eventos do funil. */
  private record LandingAnalyticsEventRow(long id, String payload, Instant occurredAt) {}

  /** Qualidade operacional usada para separar audiência comercial de verificações técnicas. */
  private enum TrafficQuality {
    HUMAN,
    AUTOMATED,
    UNKNOWN
  }

  /** Resultado explicável da classificação de tráfego da landing. */
  private record TrafficDiagnosis(TrafficQuality quality, String reason) {}

  /**
   * Classifica verificações técnicas por sinais combinados e mantém eventos sem identidade
   * first-party como desconhecidos, sem convertê-los em audiência comercial.
   */
  private static TrafficDiagnosis classifyLandingTraffic(
      Map<String, String> payload, String visitorId, boolean fakeExperiment) {
    String pageUrl = firstNonBlankStatic(payload.get("pageUrl"), "").toLowerCase();
    String userAgent = firstNonBlankStatic(payload.get("userAgent"), "").toLowerCase();
    String width = firstNonBlankStatic(payload.get("screenWidth"), "");
    String height = firstNonBlankStatic(payload.get("screenHeight"), "");
    boolean missingVisitor = visitorId == null || visitorId.isBlank();
    boolean explicitAutomation =
        "true".equalsIgnoreCase(firstNonBlankStatic(payload.get("automationSignal"), "false"));
    boolean internalRenderUrl = pageUrl.contains("/api/flows/") && pageUrl.contains("/page");
    boolean internalTestUrl = pageUrl.contains("mh_test=1") || pageUrl.contains("mh_audit=");
    boolean knownAutomationAgent =
        userAgent.contains("headlesschrome")
            || userAgent.contains("playwright")
            || userAgent.contains("puppeteer")
            || userAgent.contains("bot")
            || userAgent.contains("crawler");
    boolean canonicalMonitorViewport = "1600".equals(width) && "1200".equals(height);
    if (fakeExperiment) {
      return new TrafficDiagnosis(TrafficQuality.AUTOMATED, "FAKE_EXPERIMENT");
    }
    if (internalTestUrl) {
      return new TrafficDiagnosis(TrafficQuality.AUTOMATED, "INTERNAL_TEST_URL");
    }
    if (explicitAutomation) {
      return new TrafficDiagnosis(TrafficQuality.AUTOMATED, "BROWSER_AUTOMATION_SIGNAL");
    }
    if (knownAutomationAgent) {
      return new TrafficDiagnosis(TrafficQuality.AUTOMATED, "AUTOMATION_USER_AGENT");
    }
    if (missingVisitor && internalRenderUrl && canonicalMonitorViewport) {
      return new TrafficDiagnosis(TrafficQuality.AUTOMATED, "INTERNAL_RENDER_MONITOR");
    }
    if (missingVisitor && internalRenderUrl) {
      return new TrafficDiagnosis(TrafficQuality.AUTOMATED, "INTERNAL_RENDER_URL");
    }
    if (missingVisitor) {
      return new TrafficDiagnosis(TrafficQuality.UNKNOWN, "MISSING_VISITOR_ID");
    }
    return new TrafficDiagnosis(TrafficQuality.HUMAN, "NO_AUTOMATION_SIGNAL");
  }

  /** Identifica submissões técnicas marcadas pelo modo de homologação do Lead Portal. */
  private boolean isInternalTestCampaign(String campaignCode) {
    return INTERNAL_TEST_CAMPAIGN_CODE.equals(normalizeCampaignCode(campaignCode));
  }

  /** Retorna texto preenchido sem depender de uma instância do serviço. */
  private static String firstNonBlankStatic(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  /** Acumula os eventos de analytics de uma sessão pública da landing. */
  private static final class LandingAnalyticsSessionAccumulator {
    private final String sessionId;
    private long eventCount;
    private long pageViews;
    private long sectionViewEvents;
    private long totalVisibleMs;
    private Instant firstEventAt;
    private Instant lastEventAt;
    private String lastPageUrl;
    private String lastUserAgent;
    private String deviceType = "desktop";
    private String operatingSystem = "other";
    private Integer screenWidth;
    private Integer screenHeight;
    private TrafficQuality trafficQuality = TrafficQuality.HUMAN;
    private String trafficQualityReason = "NO_AUTOMATION_SIGNAL";
    private final Set<String> recordedMilestones = new java.util.HashSet<>();
    private final Map<String, Instant> recordedPageViews = new HashMap<>();
    private final Map<String, SectionAccumulator> sections = new LinkedHashMap<>();

    /** Cria acumulador de sessão para o identificador normalizado recebido da landing. */
    private LandingAnalyticsSessionAccumulator(String sessionId) {
      this.sessionId = sessionId;
    }

    /** Acrescenta um evento recebido na sessão e atualiza contadores de página e seção. */
    private void record(
        Instant occurredAt,
        String eventType,
        String sectionId,
        long elapsedMs,
        String pageUrl,
        String userAgent,
        String deviceType,
        String operatingSystem,
        Integer screenWidth,
        Integer screenHeight,
        TrafficDiagnosis traffic) {
      String normalizedEventType = eventType == null ? "" : eventType.trim().toLowerCase();
      if (isDeduplicatedMilestone(normalizedEventType, occurredAt, pageUrl)) {
        return;
      }
      eventCount++;
      if (traffic != null
          && (traffic.quality() == TrafficQuality.AUTOMATED
              || (traffic.quality() == TrafficQuality.UNKNOWN
                  && trafficQuality == TrafficQuality.HUMAN))) {
        trafficQuality = traffic.quality();
        trafficQualityReason = traffic.reason();
      }
      if (firstEventAt == null || (occurredAt != null && occurredAt.isBefore(firstEventAt))) {
        firstEventAt = occurredAt;
      }
      if (lastEventAt == null || (occurredAt != null && occurredAt.isAfter(lastEventAt))) {
        lastEventAt = occurredAt;
        if (pageUrl != null && !pageUrl.isBlank()) {
          lastPageUrl = pageUrl;
        }
        if (userAgent != null && !userAgent.isBlank()) {
          lastUserAgent = userAgent;
        }
      }
      if ((deviceType != null && !deviceType.isBlank())
          || (userAgent != null && !userAgent.isBlank())) {
        this.deviceType = normalizeLandingAnalyticsDeviceType(deviceType, userAgent);
      }
      if ((operatingSystem != null && !operatingSystem.isBlank())
          || (userAgent != null && !userAgent.isBlank())) {
        this.operatingSystem = normalizeLandingAnalyticsOperatingSystem(operatingSystem, userAgent);
      }
      if (screenWidth != null && screenHeight != null) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
      }
      if ("page_view".equalsIgnoreCase(eventType)) {
        pageViews++;
      }
      if ("section_view_time".equalsIgnoreCase(eventType)) {
        sectionViewEvents++;
        totalVisibleMs += elapsedMs;
        String normalizedSection =
            sectionId == null || sectionId.isBlank() ? "sem-secao" : sectionId.trim();
        sections.computeIfAbsent(normalizedSection, SectionAccumulator::new).record(elapsedMs);
      }
    }

    /** Deduplica marcos pela regra canônica sem eliminar recarregamentos válidos da página. */
    private boolean isDeduplicatedMilestone(String eventType, Instant occurredAt, String pageUrl) {
      if ("page_view".equals(eventType)) {
        String pageKey = firstNonBlankStatic(pageUrl, "sem-url");
        Instant previous = recordedPageViews.get(pageKey);
        if (previous != null
            && occurredAt != null
            && Duration.between(previous, occurredAt)
                    .abs()
                    .compareTo(Duration.ofSeconds(PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS))
                <= 0) {
          return true;
        }
        if (occurredAt == null) {
          return !recordedMilestones.add("page_view:" + pageKey);
        }
        recordedPageViews.put(pageKey, occurredAt);
        return false;
      }
      return ("video_complete".equals(eventType)
              || "video_completed".equals(eventType)
              || "checkout_click".equals(eventType)
              || "form_start".equals(eventType)
              || "form_submit".equals(eventType))
          && !recordedMilestones.add(eventType);
    }

    /** Informa se a sessão pode compor métricas comerciais e de desempenho. */
    private boolean isHuman() {
      return trafficQuality == TrafficQuality.HUMAN;
    }

    /** Retorna a classificação da sessão para auditoria e consumo pelo Operador. */
    private String trafficQuality() {
      return trafficQuality.name();
    }

    /** Retorna o motivo explicável da classificação de tráfego. */
    private String trafficQualityReason() {
      return trafficQualityReason;
    }

    /** Retorna o horário do último evento para ordenação das sessões mais recentes. */
    private Instant lastEventAt() {
      return lastEventAt;
    }

    /** Retorna o tipo de dispositivo normalizado da sessão para agregação percentual. */
    private String deviceType() {
      return deviceType;
    }

    /** Retorna o sistema operacional mobile normalizado da sessão para agregação percentual. */
    private String operatingSystem() {
      return operatingSystem;
    }

    /**
     * Retorna a quantidade de eventos de seção registrados para diagnóstico de engajamento inicial.
     */
    private long sectionViewEvents() {
      return sectionViewEvents;
    }

    /** Retorna o user-agent mais recente da sessão para diagnóstico de navegador in-app. */
    private String lastUserAgent() {
      return lastUserAgent;
    }

    /** Informa se a sessão tem dimensões de tela válidas capturadas pelo script público. */
    private boolean hasScreenSize() {
      return screenWidth != null && screenHeight != null;
    }

    /** Retorna a largura de tela da sessão para agregação de resoluções. */
    private Integer screenWidth() {
      return screenWidth;
    }

    /** Retorna a altura de tela da sessão para agregação de resoluções. */
    private Integer screenHeight() {
      return screenHeight;
    }

    /** Retorna a chave textual de resolução da sessão. */
    private String screenSizeKey() {
      return screenWidth + "x" + screenHeight;
    }

    /** Retorna o rótulo textual da resolução da sessão. */
    private String screenSizeLabel() {
      return hasScreenSize() ? screenSizeKey() + " px" : null;
    }

    /** Converte o acumulador interno em DTO serializável pela API. */
    private ExperimentLandingAnalyticsSessionDto toDto() {
      List<ExperimentLandingAnalyticsSectionDto> topSections =
          sections.values().stream()
              .sorted(Comparator.comparingLong(SectionAccumulator::visibleMs).reversed())
              .limit(5)
              .map(SectionAccumulator::toDto)
              .collect(Collectors.toCollection(ArrayList::new));
      return new ExperimentLandingAnalyticsSessionDto(
          sessionId,
          eventCount,
          pageViews,
          sectionViewEvents,
          totalVisibleMs,
          firstEventAt,
          lastEventAt,
          lastPageUrl,
          lastUserAgent,
          deviceType,
          landingAnalyticsDeviceLabel(deviceType),
          operatingSystem,
          landingAnalyticsOperatingSystemLabel(operatingSystem),
          screenWidth,
          screenHeight,
          screenSizeLabel(),
          trafficQuality(),
          trafficQualityReason(),
          topSections);
    }
  }

  /** Acumula sessões por resolução de tela capturada na landing. */
  private static final class ScreenSizeAccumulator {
    private final Integer width;
    private final Integer height;
    private long sessions;

    private ScreenSizeAccumulator(Integer width, Integer height) {
      this.width = width;
      this.height = height;
    }

    /** Soma uma sessão à resolução de tela. */
    private void record() {
      sessions++;
    }

    /** Retorna a chave canônica da resolução para a API. */
    private String key() {
      return width + "x" + height;
    }

    /** Retorna o rótulo amigável da resolução para a UI. */
    private String label() {
      return key() + " px";
    }

    /** Retorna a largura da tela em pixels CSS. */
    private Integer width() {
      return width;
    }

    /** Retorna a altura da tela em pixels CSS. */
    private Integer height() {
      return height;
    }

    /** Retorna a quantidade de sessões nesta resolução. */
    private long sessions() {
      return sessions;
    }
  }

  /** Acumula tempo visível e volume de eventos por seção da landing. */
  private static final class SectionAccumulator {
    private final String sectionId;
    private long visibleMs;
    private long events;

    /** Cria acumulador de seção para o identificador normalizado da landing. */
    private SectionAccumulator(String sectionId) {
      this.sectionId = sectionId;
    }

    /** Soma um evento de tempo visível para a seção. */
    private void record(long elapsedMs) {
      events++;
      visibleMs += elapsedMs;
    }

    /** Retorna o tempo visível acumulado para ordenação das seções. */
    private long visibleMs() {
      return visibleMs;
    }

    /** Converte o acumulador de seção em DTO serializável pela API. */
    private ExperimentLandingAnalyticsSectionDto toDto() {
      return new ExperimentLandingAnalyticsSectionDto(sectionId, visibleMs, events);
    }
  }

  /**
   * Métrica agregada usada para transportar total, contagem única e último evento das consultas
   * SQL.
   */
  private record AggregatedMetric(long total, Long uniqueCount, Instant lastEvent) {}

  /** Métrica consolidada do funil PDE/MUSA já atribuída ao experimento. */
  private record PdeMembershipMetric(
      long pdeEntries,
      long videoPartial,
      long videoComplete,
      long loginStarted,
      long paywallViewed,
      long checkoutIntent,
      long subscriptionApproved,
      long accessReleased,
      long firstUse,
      Instant lastEventAt) {
    /** Cria métrica zerada quando o fake ainda não recebeu tráfego atribuído ao experimento. */
    private static PdeMembershipMetric empty() {
      return new PdeMembershipMetric(0, 0, 0, 0, 0, 0, 0, 0, 0, null);
    }
  }
}
