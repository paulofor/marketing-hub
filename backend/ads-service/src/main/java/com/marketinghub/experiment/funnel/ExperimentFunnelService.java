package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.RegisterExperimentFunnelEventRequest;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDeviceDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsOperatingSystemDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsScreenSizeDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSectionDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSessionDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsVisitorDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsVisitorsDto;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionEngagementContractV1;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import com.marketinghub.leadportal.dto.RegisterLeadPortalSubmissionRequest;
import com.marketinghub.model.Lead;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository.StageAggregation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço que consolida as etapas do funil de vendas de um experimento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentFunnelService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentFunnelEventRepository eventRepository;
    private final ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;
    private final LeadRepository leadRepository;
    private final JdbcTemplate jdbcTemplate;

    static final String FLOW_SCOPE_CONDITION = """
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

    /**
     * Consolida as métricas automáticas e eventos registrados por etapa do funil.
     */
    public List<ExperimentFunnelStageDto> summarize(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages = bootstrapStages();
        Instant baseline = resolveBaseline(experiment);

        applyManualEvents(experiment.getId(), baseline, stages);
        applyAutomaticMetrics(experiment.getId(), baseline, stages);
        fillDefaultSourceWhenMissing(stages);

        return stages.values().stream()
                .sorted(Comparator.comparingInt(ExperimentFunnelStageDto::getOrder))
                .toList();
    }

    /**
     * Consolida sessões e eventos de analytics da landing publicados no experimento.
     */
    public ExperimentLandingAnalyticsDto summarizeLandingAnalytics(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        Instant baseline = resolveBaseline(experiment);
        List<LandingAnalyticsEventRow> rows = fetchLandingAnalyticsEvents(experimentId, baseline);
        Map<String, LandingAnalyticsSessionAccumulator> sessions = new LinkedHashMap<>();
        long pageViews = 0;
        long sectionViewEvents = 0;
        long totalVisibleMs = 0;
        Instant lastEventAt = null;

        for (LandingAnalyticsEventRow row : rows) {
            Map<String, String> payload = parseDelimitedPayload(row.payload());
            String sessionId = firstNonBlank(payload.get("sessionId"), "sem-sessao");
            String eventType = firstNonBlank(payload.get("eventType"), "desconhecido");
            String sectionId = payload.get("sectionId");
            String pageUrl = payload.get("pageUrl");
            String userAgent = payload.get("userAgent");
            String deviceType = normalizeLandingAnalyticsDeviceType(payload.get("deviceType"), userAgent);
            String operatingSystem = normalizeLandingAnalyticsOperatingSystem(payload.get("operatingSystem"), userAgent);
            Integer screenWidth = parsePositiveInteger(payload.get("screenWidth"));
            Integer screenHeight = parsePositiveInteger(payload.get("screenHeight"));
            long elapsedMs = parseLong(payload.get("elapsedMs"));

            LandingAnalyticsSessionAccumulator session = sessions.computeIfAbsent(
                    sessionId, LandingAnalyticsSessionAccumulator::new);
            session.record(row.occurredAt(), eventType, sectionId, elapsedMs, pageUrl, userAgent, deviceType,
                    operatingSystem, screenWidth, screenHeight);

            if ("page_view".equalsIgnoreCase(eventType)) {
                pageViews++;
            }
            if ("section_view_time".equalsIgnoreCase(eventType)) {
                sectionViewEvents++;
                totalVisibleMs += elapsedMs;
            }
            lastEventAt = max(lastEventAt, row.occurredAt());
        }

        List<ExperimentLandingAnalyticsSessionDto> sessionDtos = sessions.values().stream()
                .sorted(Comparator.comparing(LandingAnalyticsSessionAccumulator::lastEventAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .map(LandingAnalyticsSessionAccumulator::toDto)
                .toList();
        long averageVisibleMsPerSession = sessions.isEmpty() ? 0 : totalVisibleMs / sessions.size();
        List<ExperimentLandingAnalyticsDeviceDto> deviceBreakdown = buildDeviceBreakdown(sessions);
        List<ExperimentLandingAnalyticsOperatingSystemDto> mobileOperatingSystemBreakdown =
                buildMobileOperatingSystemBreakdown(sessions);
        List<ExperimentLandingAnalyticsScreenSizeDto> screenSizeBreakdown = buildScreenSizeBreakdown(sessions);
        ExperimentLandingAnalyticsVisitorsDto visitors = summarizeLandingAnalyticsVisitors(experiment, baseline);
        return new ExperimentLandingAnalyticsDto(
                rows.size(),
                sessions.size(),
                pageViews,
                sectionViewEvents,
                totalVisibleMs,
                averageVisibleMsPerSession,
                lastEventAt,
                deviceBreakdown,
                mobileOperatingSystemBreakdown,
                screenSizeBreakdown,
                visitors,
                sessionDtos);
    }

    /**
     * Consolida visitantes prováveis recorrentes da landing para apoiar decisão comercial do experimento.
     */
    public ExperimentLandingAnalyticsVisitorsDto summarizeLandingAnalyticsVisitors(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        Instant baseline = resolveBaseline(experiment);
        return summarizeLandingAnalyticsVisitors(experiment, baseline);
    }

    /**
     * Agrega visitantes prováveis a partir dos page_views normalizados já deduplicados.
     */
    private ExperimentLandingAnalyticsVisitorsDto summarizeLandingAnalyticsVisitors(Experiment experiment,
                                                                                     Instant baseline) {
        List<ExperimentLandingAnalyticsVisitorDto> visitors = landingAnalyticsEventRepository
                .aggregateVisitorsByExperiment(experiment.getId(), baseline)
                .stream()
                .map(this::toVisitorDto)
                .toList();
        long recurrentVisitors = visitors.stream()
                .filter(ExperimentLandingAnalyticsVisitorDto::recurrent)
                .count();
        return new ExperimentLandingAnalyticsVisitorsDto(
                visitors.size(),
                recurrentVisitors,
                visitors.size() - recurrentVisitors,
                visitors);
    }

    /**
     * Converte a agregação SQL em DTO público, mascarando o visitorId antes de responder à API.
     */
    private ExperimentLandingAnalyticsVisitorDto toVisitorDto(
            ExperimentLandingAnalyticsEventRepository.VisitorRecurrenceProjection projection) {
        long intervalSeconds = secondsBetween(projection.getFirstAccessAt(), projection.getLastAccessAt());
        boolean recurrent = projection.getTotalSessions() >= 2
                || (projection.getValidPageViews() >= 2
                        && intervalSeconds > PAGE_VIEW_DEDUPLICATION_WINDOW_SECONDS);
        String deviceType = normalizeLandingAnalyticsDeviceType(null, projection.getLastUserAgent());
        return new ExperimentLandingAnalyticsVisitorDto(
                maskVisitorId(projection.getVisitorId()),
                projection.getTotalSessions(),
                projection.getValidPageViews(),
                projection.getFirstAccessAt(),
                projection.getLastAccessAt(),
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

    /**
     * Registra manualmente um evento do funil para o experimento informado.
     */
    @Transactional
    public void registerEvent(Long experimentId, RegisterExperimentFunnelEventRequest request) {
        if (request == null || request.stage() == null) {
            throw new IllegalArgumentException("Etapa do funil é obrigatória");
        }
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        Lead lead = resolveLead(request.leadId());

        ExperimentFunnelEvent event = ExperimentFunnelEvent.builder()
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
     * Apaga eventos de teste do funil, incluindo analytics de sessão, e define o marco temporal de reinício.
     */
    @Transactional
    public Instant resetFunnel(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        int deletedNormalizedLandingAnalytics = landingAnalyticsEventRepository.deleteByExperimentId(experimentId);
        int deletedLandingAnalytics = eventRepository.deleteByExperimentIdAndSource(
                experimentId,
                ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
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

    /**
     * Registra a visualização do formulário recebida pelo tracking público do Lead Portal.
     */
    @Transactional
    public void registerFormRenderCompleted(String flowSlug, String visitorId, String campaignCode) {
        if (flowSlug == null || flowSlug.isBlank()) {
            throw new IllegalArgumentException("Slug do fluxo é obrigatório");
        }
        Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

        String sanitizedVisitorId = visitorId == null ? null : visitorId.trim();
        String payload = sanitizedVisitorId == null || sanitizedVisitorId.isBlank()
                ? null
                : "visitorId=" + sanitizedVisitorId;

        ExperimentFunnelEvent event = ExperimentFunnelEvent.builder()
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
     * Registra de forma idempotente o envio do formulário recebido pelo Lead Portal.
     */
    @Transactional
    public boolean registerFormSubmission(String flowSlug, RegisterLeadPortalSubmissionRequest request) {
        if (flowSlug == null || flowSlug.isBlank()) {
            throw new IllegalArgumentException("Slug do fluxo é obrigatório");
        }
        if (request == null || request.submissionId() == null || request.submissionId().isBlank()) {
            throw new IllegalArgumentException("ID da submissão é obrigatório");
        }

        Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

        String payload = "submissionId=" + request.submissionId().trim();
        boolean duplicated = eventRepository.existsByExperimentIdAndStageAndSourceAndPayload(
                experiment.getId(),
                ExperimentFunnelStage.ENVIO_FORM,
                ExperimentFunnelEventRepository.SUBMISSION_SOURCE,
                payload);
        if (duplicated) {
            return false;
        }
        Instant occurredAt = Optional.ofNullable(request.submittedAt()).orElse(Instant.now());

        ExperimentFunnelEvent event = ExperimentFunnelEvent.builder()
                .experiment(experiment)
                .stage(ExperimentFunnelStage.ENVIO_FORM)
                .source(ExperimentFunnelEventRepository.SUBMISSION_SOURCE)
                .campaignCode(normalizeCampaignCode(request.campaignCode()))
                .payload(payload)
                .occurredAt(occurredAt)
                .build();
        eventRepository.save(event);
        return true;
    }

    /**
     * Converte o contrato v1 de submissão do Lead Portal para o registro idempotente legado.
     */
    @Transactional
    public boolean registerFormSubmission(String flowSlug, LeadPortalSubmissionEngagementContractV1 request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload de submissão é obrigatório");
        }
        if (!LeadPortalSubmissionEngagementContractV1.VERSION.equals(request.contractVersion())) {
            throw new IllegalArgumentException("Versão de contrato não suportada: " + request.contractVersion());
        }
        RegisterLeadPortalSubmissionRequest legacyRequest = new RegisterLeadPortalSubmissionRequest(
                request.submissionId(),
                request.submittedAt(),
                request.campaignCode());
        return registerFormSubmission(flowSlug, legacyRequest);
    }


    /**
     * Registra analytics da landing publicada, preserva o evento bruto e normaliza o contrato por visitante provável.
     */
    @Transactional
    public void registerLandingPageAnalyticsEvent(String flowSlug, RegisterLandingPageAnalyticsEventRequest request) {
        if (flowSlug == null || flowSlug.isBlank()) {
            throw new IllegalArgumentException("Slug do fluxo é obrigatório");
        }
        log.info("landing_page_analytics_raw flowSlug={} payload={}", flowSlug.trim(), request);
        validateLandingAnalyticsRequest(request);
        Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

        Long elapsedMs = resolveLandingAnalyticsElapsedMs(request);
        String eventType = request.eventType().trim();
        Instant occurredAt = Optional.ofNullable(request.occurredAt()).orElse(Instant.now());
        String payload = buildLandingAnalyticsPayload(request, elapsedMs);

        log.info("landing_page_analytics experimentId={} flowSlug={} eventId={} visitorId={} eventType={} sectionId={} elapsedMs={} sessionId={} pageUrl={} occurredAt={} userAgent={} deviceType={}",
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
                normalizeLandingAnalyticsDeviceType(request.deviceType(), request.userAgent()));

        ExperimentFunnelStage stage = resolveStageForLandingAnalyticsEvent(eventType);
        if (stage != null) {
            ExperimentFunnelEvent event = ExperimentFunnelEvent.builder()
                    .experiment(experiment)
                    .stage(stage)
                    .source(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE)
                    .campaignCode(null)
                    .payload(payload)
                    .occurredAt(occurredAt)
                    .build();
            ExperimentFunnelEvent savedEvent = eventRepository.save(event);
            saveNormalizedLandingAnalyticsEvent(experiment, savedEvent, request, occurredAt);
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
            validateRequiredLandingAnalyticsField(request.sessionId(), "sessionId é obrigatório para page_view");
            validateRequiredLandingAnalyticsField(request.pageUrl(), "pageUrl é obrigatório para page_view");
        }
        if ("section_view_time".equalsIgnoreCase(eventType)) {
            validateRequiredLandingAnalyticsField(request.sessionId(), "sessionId é obrigatório para section_view_time");
            validateRequiredLandingAnalyticsField(request.sectionId(), "sectionId é obrigatório para section_view_time");
            if (resolveLandingAnalyticsElapsedMs(request) == null) {
                throw new IllegalArgumentException("elapsedMs é obrigatório para section_view_time");
            }
        }
    }

    /**
     * Rejeita campos textuais obrigatórios quando estão ausentes ou em branco.
     */
    private void validateRequiredLandingAnalyticsField(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Resolve a duração canônica do evento mantendo compatibilidade com o campo legado visibleMs.
     */
    private Long resolveLandingAnalyticsElapsedMs(RegisterLandingPageAnalyticsEventRequest request) {
        return request.elapsedMs() != null ? request.elapsedMs() : request.visibleMs();
    }

    /**
     * Salva ou atualiza o evento normalizado, ignorando page_view duplicado na janela canônica curta.
     */
    private void saveNormalizedLandingAnalyticsEvent(Experiment experiment,
                                                    ExperimentFunnelEvent funnelEvent,
                                                    RegisterLandingPageAnalyticsEventRequest request,
                                                    Instant occurredAt) {
        String visitorId = normalizeNullableText(request.visitorId(), 128);
        String sessionId = normalizeNullableText(request.sessionId(), 128);
        String eventType = normalizeNullableText(request.eventType(), 64);
        String pageUrl = normalizeNullableText(request.pageUrl(), 2048);
        if (isDuplicatedPageView(experiment.getId(), request, visitorId, sessionId, eventType, pageUrl, occurredAt)) {
            log.info("landing_page_analytics_deduplicated experimentId={} eventId={} visitorId={} sessionId={} eventType={} pageUrl={} occurredAt={} windowSeconds={}",
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
        ExperimentLandingAnalyticsEvent normalizedEvent = landingAnalyticsEventRepository
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
        normalizedEvent.setOccurredAt(occurredAt);
        landingAnalyticsEventRepository.save(normalizedEvent);
    }

    /**
     * Verifica se o evento é um page_view duplicado por visitante, sessão e URL dentro da janela canônica.
     */
    private boolean isDuplicatedPageView(Long experimentId,
                                         RegisterLandingPageAnalyticsEventRequest request,
                                         String visitorId,
                                         String sessionId,
                                         String eventType,
                                         String pageUrl,
                                         Instant occurredAt) {
        if (!"page_view".equalsIgnoreCase(eventType) || visitorId == null || sessionId == null || pageUrl == null) {
            return false;
        }
        if (landingAnalyticsEventRepository.findFirstByExperimentIdAndEventId(
                experimentId,
                request.eventId().trim()).isPresent()) {
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

    /**
     * Normaliza texto para colunas relacionais, retornando null quando o valor está vazio.
     */
    private String normalizeNullableText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace("\r", " ").replace("\n", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }


    /**
     * Resolve o experimento pelo slug, cobrindo tanto vínculo interno quanto landing publicada externamente.
     */
    private Experiment resolveExperimentByFlowSlug(String flowSlug) {
        return experimentRepository.findFirstByLeadPortalFlowSlug(flowSlug)
                .or(() -> experimentRepository.findFirstByFollowUpActionUrlFlowSlug(flowSlug))
                .orElseThrow(() -> new IllegalArgumentException("Fluxo não vinculado a experimento"));
    }

    /**
     * Mapeia eventos de analytics da landing para a etapa consolidada do funil.
     */
    private ExperimentFunnelStage resolveStageForLandingAnalyticsEvent(String eventType) {
        if ("page_view".equalsIgnoreCase(eventType)) {
            return ExperimentFunnelStage.VISUALIZACAO_FORM;
        }
        if ("section_view_time".equalsIgnoreCase(eventType)) {
            return ExperimentFunnelStage.VISUALIZACAO_FORM;
        }
        return null;
    }

    /**
     * Monta um payload textual rastreável sem serializar JSON dentro de JSON.
     */
    private String buildLandingAnalyticsPayload(RegisterLandingPageAnalyticsEventRequest request, Long elapsedMs) {
        String sectionId = sanitizePayloadValue(request.sectionId());
        String visitorId = sanitizePayloadValue(request.visitorId());
        String sessionId = sanitizePayloadValue(request.sessionId());
        String url = sanitizePayloadValue(request.pageUrl());
        String userAgent = sanitizePayloadValue(request.userAgent());
        String deviceType = sanitizePayloadValue(
                normalizeLandingAnalyticsDeviceType(request.deviceType(), request.userAgent()));
        String operatingSystem = sanitizePayloadValue(
                normalizeLandingAnalyticsOperatingSystem(request.operatingSystem(), request.userAgent()));
        String screenWidth = request.screenWidth() == null ? "" : request.screenWidth().toString();
        String screenHeight = request.screenHeight() == null ? "" : request.screenHeight().toString();
        String duration = elapsedMs == null ? "" : elapsedMs.toString();
        return "eventId=" + sanitizePayloadValue(request.eventId())
                + ";eventType=" + sanitizePayloadValue(request.eventType())
                + ";visitorId=" + visitorId
                + ";sessionId=" + sessionId
                + ";sectionId=" + sectionId
                + ";elapsedMs=" + duration
                + ";pageUrl=" + url
                + ";userAgent=" + userAgent
                + ";deviceType=" + deviceType
                + ";operatingSystem=" + operatingSystem
                + ";screenWidth=" + sanitizePayloadValue(screenWidth)
                + ";screenHeight=" + sanitizePayloadValue(screenHeight);
    }

    /**
     * Resolve o lead opcional associado ao evento manual quando o identificador foi informado.
     */
    private Lead resolveLead(UUID leadId) {
        if (leadId == null) {
            return null;
        }
        return leadRepository.findById(leadId).orElse(null);
    }

    /**
     * Inicializa todas as etapas do funil com contadores zerados para consolidação.
     */
    private Map<ExperimentFunnelStage, ExperimentFunnelStageDto> bootstrapStages() {
        Map<ExperimentFunnelStage, ExperimentFunnelStageDto> result = new LinkedHashMap<>();
        Arrays.stream(ExperimentFunnelStage.values()).forEach(stage -> {
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

    /**
     * Aplica os eventos manuais registrados no repositório às etapas consolidadas.
     */
    private void applyManualEvents(Long experimentId, Instant baseline, Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
        for (StageAggregation agg : eventRepository.aggregateManualByExperiment(experimentId, baseline)) {
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
    private void applyAutomaticMetrics(Long experimentId, Instant baseline, Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
        mergeMetric(stages, ExperimentFunnelStage.VISUALIZACAO_ANUNCIO,
                fetchSingleMetric("""
                        SELECT COALESCE(SUM(impressions), 0) AS total,
                               NULL AS unique_count,
                               MAX(updated_at) AS last_event
                        FROM experiment_campaign_metric
                        WHERE experiment_id = ?
                          AND (? IS NULL OR updated_at > ?)
                        """, experimentId, baseline, baseline),
                "Impressões vindas do Facebook Ads (experiment_campaign_metric)");

        mergeMetric(stages, ExperimentFunnelStage.ACESSO_FORM_LEAD,
                fetchSingleMetric("""
                        SELECT COALESCE(SUM(clicks), 0) AS total,
                               NULL AS unique_count,
                               MAX(updated_at) AS last_event
                        FROM experiment_campaign_metric
                        WHERE experiment_id = ?
                          AND (? IS NULL OR updated_at > ?)
                        """, experimentId, baseline, baseline),
                "Cliques do anúncio para o formulário (experiment_campaign_metric)");

        mergeMetric(stages, ExperimentFunnelStage.VISUALIZACAO_FORM,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               NULL AS unique_count,
                               MAX(occurred_at) AS last_event
                        FROM experiment_funnel_event
                        WHERE experiment_id = ?
                          AND stage = 'VISUALIZACAO_FORM'
                          AND source IN (?, ?)
                          AND (? IS NULL OR occurred_at > ?)
                        """, experimentId,
                        ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE,
                        ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
                        baseline, baseline),
                "Visualizações registradas pelo Lead Portal e analytics da landing (experiment_funnel_event)");

        mergeMetric(stages, ExperimentFunnelStage.ENVIO_FORM,
                fetchSingleMetric("""
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
                            UNION ALL
                            SELECT CAST(SUBSTRING(efe.payload, CHAR_LENGTH('submissionId=') + 1) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   efe.lead_id,
                                   efe.occurred_at AS submitted_at
                            FROM experiment_funnel_event efe
                            WHERE efe.experiment_id = ?
                              AND efe.stage = 'ENVIO_FORM'
                              AND efe.source = ?
                              AND efe.payload LIKE 'submissionId=%%'
                        ) submissions
                        WHERE canonical_submission_id IS NOT NULL
                          AND canonical_submission_id <> ''
                          AND (? IS NULL OR submitted_at > ?)
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId, experimentId, experimentId,
                        ExperimentFunnelEventRepository.SUBMISSION_SOURCE, baseline, baseline),
                "Envios do formulário (lead_portal_submission + flow_submissions + experiment_funnel_event)");

        mergeMetric(stages, ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT s.id) AS unique_count,
                               MAX(p.email_opened_at) AS last_event
                        FROM flow_submission_image_package p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND p.email_opened_at IS NOT NULL
                          AND (? IS NULL OR p.email_opened_at > ?)
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId, baseline, baseline),
                "Aberturas de e-mail de amostra (flow_submission_image_package.email_opened_at)");

        mergeMetric(stages, ExperimentFunnelStage.ACESSO_CHECKOUT,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT p.submission_id) AS unique_count,
                               MAX(p.checkout_accessed_at) AS last_event
                        FROM lead_portal_purchase p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND p.checkout_accessed_at IS NOT NULL
                          AND (? IS NULL OR p.checkout_accessed_at > ?)
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId, baseline, baseline),
                "Acessos ao checkout registrados via tracking público (lead_portal_purchase.checkout_accessed_at)");

        mergeMetric(stages, ExperimentFunnelStage.COMPRA,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT p.submission_id) AS unique_count,
                               MAX(COALESCE(p.payment_approved_at, p.updated_at)) AS last_event
                        FROM lead_portal_purchase p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND (p.payment_approved_at IS NOT NULL OR p.mp_status = 'approved')
                          AND (? IS NULL OR COALESCE(p.payment_approved_at, p.updated_at) > ?)
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId, baseline, baseline),
                "Pagamentos aprovados (lead_portal_purchase)");

        mergeMetric(stages, ExperimentFunnelStage.ABERTURA_EMAIL_COMPRA,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT d.submission_id) AS unique_count,
                               MAX(el.opened_at) AS last_event
                        FROM lead_portal_premium_delivery d
                        JOIN flow_submissions s ON s.id = d.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        LEFT JOIN email_log el ON el.request_id = d.email_request_id
                        WHERE %s
                          AND d.email_request_id IS NOT NULL
                          AND el.opened_at IS NOT NULL
                          AND (? IS NULL OR el.opened_at > ?)
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId, baseline, baseline),
                "Abertura do e-mail de entrega (lead_portal_premium_delivery -> email_log)");

        mergeMetric(stages, ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT p.submission_id) AS unique_count,
                               MAX(p.images_viewed_at) AS last_event
                        FROM flow_submission_image_package p
                        JOIN flow_submissions s ON s.id = p.submission_id
                        JOIN lead_portal_flow f ON f.slug = s.flow_slug
                        WHERE %s
                          AND p.payment_purchase_id IS NOT NULL
                          AND p.images_viewed_at IS NOT NULL
                          AND (? IS NULL OR p.images_viewed_at > ?)
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId, baseline, baseline),
                "Downloads/visualizações do material pago (flow_submission_image_package.images_viewed_at)");
    }

    /**
     * Mescla uma métrica automática na etapa do funil correspondente.
     */
    private void mergeMetric(Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages,
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
     * Executa consulta agregada única e converte o resultado para métrica interna.
     */
    private AggregatedMetric fetchSingleMetric(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            long total = rs.getLong("total");
            Long unique = (Long) rs.getObject("unique_count");
            Timestamp ts = rs.getTimestamp("last_event");
            Instant last = ts != null ? ts.toInstant() : null;
            return new AggregatedMetric(total, unique, last);
        }, args);
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

    /**
     * Normaliza o código de campanha para o tamanho aceito pelo banco.
     */
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

    /**
     * Preenche a origem padrão em etapas que possuem apenas eventos manuais.
     */
    private void fillDefaultSourceWhenMissing(Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
        stages.values().forEach(dto -> {
            if (dto.getSource() == null && dto.getManualCount() > 0) {
                dto.setSource("Eventos manuais registrados na aplicação");
            }
        });
    }

    /**
     * Soma contadores opcionais preservando null quando ambos estão ausentes.
     */
    private Long sum(Long a, Long b) {
        if (a == null && b == null) return null;
        return Optional.ofNullable(a).orElse(0L) + Optional.ofNullable(b).orElse(0L);
    }

    /**
     * Retorna o instante mais recente entre dois valores opcionais.
     */
    private Instant max(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    /**
     * Normaliza valores do payload textual para preservar o delimitador operacional entre campos.
     */
    private String sanitizePayloadValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .replace(";", ",")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    /**
     * Consolida a distribuição percentual de sessões por tipo de dispositivo.
     */
    private List<ExperimentLandingAnalyticsDeviceDto> buildDeviceBreakdown(
            Map<String, LandingAnalyticsSessionAccumulator> sessions) {
        long totalSessions = sessions.size();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("mobile", 0L);
        counts.put("desktop", 0L);
        counts.put("tablet", 0L);
        sessions.values().forEach(session -> counts.compute(
                session.deviceType(),
                (key, count) -> (count == null ? 0 : count) + 1));
        return counts.entrySet().stream()
                .filter(entry -> "mobile".equals(entry.getKey())
                        || "desktop".equals(entry.getKey())
                        || "tablet".equals(entry.getKey()))
                .map(entry -> new ExperimentLandingAnalyticsDeviceDto(
                        entry.getKey(),
                        landingAnalyticsDeviceLabel(entry.getKey()),
                        entry.getValue(),
                        totalSessions == 0 ? 0 : Math.round((entry.getValue() * 10000.0) / totalSessions) / 100.0))
                .toList();
    }

    /**
     * Consolida a distribuição percentual de sessões mobile por sistema operacional.
     */
    private List<ExperimentLandingAnalyticsOperatingSystemDto> buildMobileOperatingSystemBreakdown(
            Map<String, LandingAnalyticsSessionAccumulator> sessions) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ios", 0L);
        counts.put("android", 0L);
        counts.put("other", 0L);
        sessions.values().stream()
                .filter(session -> "mobile".equals(session.deviceType()))
                .forEach(session -> counts.compute(
                        session.operatingSystem(),
                        (key, count) -> (count == null ? 0 : count) + 1));
        long mobileSessions = counts.values().stream().mapToLong(Long::longValue).sum();
        return counts.entrySet().stream()
                .map(entry -> new ExperimentLandingAnalyticsOperatingSystemDto(
                        entry.getKey(),
                        landingAnalyticsOperatingSystemLabel(entry.getKey()),
                        entry.getValue(),
                        mobileSessions == 0 ? 0 : Math.round((entry.getValue() * 10000.0) / mobileSessions) / 100.0))
                .toList();
    }

    /**
     * Consolida a distribuição percentual das principais resoluções de tela por sessão.
     */
    private List<ExperimentLandingAnalyticsScreenSizeDto> buildScreenSizeBreakdown(
            Map<String, LandingAnalyticsSessionAccumulator> sessions) {
        long totalSessionsWithScreen = sessions.values().stream()
                .filter(LandingAnalyticsSessionAccumulator::hasScreenSize)
                .count();
        Map<String, ScreenSizeAccumulator> counts = new LinkedHashMap<>();
        sessions.values().stream()
                .filter(LandingAnalyticsSessionAccumulator::hasScreenSize)
                .forEach(session -> counts.computeIfAbsent(
                        session.screenSizeKey(),
                        key -> new ScreenSizeAccumulator(session.screenWidth(), session.screenHeight()))
                        .record());
        return counts.values().stream()
                .sorted(Comparator.comparingLong(ScreenSizeAccumulator::sessions).reversed())
                .limit(8)
                .map(screen -> new ExperimentLandingAnalyticsScreenSizeDto(
                        screen.key(),
                        screen.label(),
                        screen.width(),
                        screen.height(),
                        screen.sessions(),
                        totalSessionsWithScreen == 0
                                ? 0
                                : Math.round((screen.sessions() * 10000.0) / totalSessionsWithScreen) / 100.0))
                .toList();
    }

    /**
     * Normaliza o tipo de dispositivo enviado pela landing, usando user-agent como fallback.
     */
    private static String normalizeLandingAnalyticsDeviceType(String rawDeviceType, String userAgent) {
        if (rawDeviceType != null && !rawDeviceType.isBlank()) {
            String normalized = rawDeviceType.trim().toLowerCase();
            if ("mobile".equals(normalized) || "tablet".equals(normalized) || "desktop".equals(normalized)) {
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
    private static String normalizeLandingAnalyticsOperatingSystem(String rawOperatingSystem, String userAgent) {
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
        if (normalizedUserAgent.contains("iphone") || normalizedUserAgent.contains("ipod")
                || normalizedUserAgent.contains("ipad")) {
            return "ios";
        }
        if (normalizedUserAgent.contains("android")) {
            return "android";
        }
        return "other";
    }

    /**
     * Retorna o rótulo do sistema operacional mobile exibido no painel de analytics.
     */
    private static String landingAnalyticsOperatingSystemLabel(String operatingSystem) {
        return switch (normalizeLandingAnalyticsOperatingSystem(operatingSystem, null)) {
            case "ios" -> "iOS";
            case "android" -> "Android";
            default -> "Outros";
        };
    }

    /**
     * Retorna o rótulo do dispositivo exibido no painel de analytics.
     */
    private static String landingAnalyticsDeviceLabel(String deviceType) {
        return switch (normalizeLandingAnalyticsDeviceType(deviceType, null)) {
            case "mobile" -> "Mobile";
            case "tablet" -> "Tablet";
            default -> "Computador";
        };
    }

    /**
     * Busca no repositório centralizado os eventos de analytics da landing para o marco temporal atual.
     */
    private List<LandingAnalyticsEventRow> fetchLandingAnalyticsEvents(Long experimentId, Instant baseline) {
        return eventRepository.findLandingAnalyticsEvents(
                        experimentId,
                        ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
                        baseline,
                        PageRequest.of(0, 2000))
                .stream()
                .map(event -> new LandingAnalyticsEventRow(event.getId(), event.getPayload(), event.getOccurredAt()))
                .toList();
    }

    /**
     * Converte o payload textual em pares chave/valor sem depender de JSON serializado em campo textual.
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

    /**
     * Retorna o primeiro valor textual preenchido ou o fallback operacional.
     */
    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

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
     * Converte números textuais positivos de tela para Integer, retornando null quando ausentes ou inválidos.
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
     * Linha mínima de evento de analytics retornada da tabela de eventos do funil.
     */
    private record LandingAnalyticsEventRow(long id, String payload, Instant occurredAt) {
    }

    /**
     * Acumula os eventos de analytics de uma sessão pública da landing.
     */
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
        private final Map<String, SectionAccumulator> sections = new LinkedHashMap<>();

        /**
         * Cria acumulador de sessão para o identificador normalizado recebido da landing.
         */
        private LandingAnalyticsSessionAccumulator(String sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * Acrescenta um evento recebido na sessão e atualiza contadores de página e seção.
         */
        private void record(Instant occurredAt, String eventType, String sectionId, long elapsedMs,
                            String pageUrl, String userAgent, String deviceType, String operatingSystem,
                            Integer screenWidth, Integer screenHeight) {
            eventCount++;
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
            this.deviceType = normalizeLandingAnalyticsDeviceType(deviceType, userAgent);
            this.operatingSystem = normalizeLandingAnalyticsOperatingSystem(operatingSystem, userAgent);
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
                String normalizedSection = sectionId == null || sectionId.isBlank() ? "sem-secao" : sectionId.trim();
                sections.computeIfAbsent(normalizedSection, SectionAccumulator::new).record(elapsedMs);
            }
        }

        /**
         * Retorna o horário do último evento para ordenação das sessões mais recentes.
         */
        private Instant lastEventAt() {
            return lastEventAt;
        }

        /**
         * Retorna o tipo de dispositivo normalizado da sessão para agregação percentual.
         */
        private String deviceType() {
            return deviceType;
        }

        /**
         * Retorna o sistema operacional mobile normalizado da sessão para agregação percentual.
         */
        private String operatingSystem() {
            return operatingSystem;
        }

        /**
         * Informa se a sessão tem dimensões de tela válidas capturadas pelo script público.
         */
        private boolean hasScreenSize() {
            return screenWidth != null && screenHeight != null;
        }

        /**
         * Retorna a largura de tela da sessão para agregação de resoluções.
         */
        private Integer screenWidth() {
            return screenWidth;
        }

        /**
         * Retorna a altura de tela da sessão para agregação de resoluções.
         */
        private Integer screenHeight() {
            return screenHeight;
        }

        /**
         * Retorna a chave textual de resolução da sessão.
         */
        private String screenSizeKey() {
            return screenWidth + "x" + screenHeight;
        }

        /**
         * Retorna o rótulo textual da resolução da sessão.
         */
        private String screenSizeLabel() {
            return hasScreenSize() ? screenSizeKey() + " px" : null;
        }

        /**
         * Converte o acumulador interno em DTO serializável pela API.
         */
        private ExperimentLandingAnalyticsSessionDto toDto() {
            List<ExperimentLandingAnalyticsSectionDto> topSections = sections.values().stream()
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
                    topSections);
        }
    }

    /**
     * Acumula sessões por resolução de tela capturada na landing.
     */
    private static final class ScreenSizeAccumulator {
        private final Integer width;
        private final Integer height;
        private long sessions;

        private ScreenSizeAccumulator(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Soma uma sessão à resolução de tela.
         */
        private void record() {
            sessions++;
        }

        /**
         * Retorna a chave canônica da resolução para a API.
         */
        private String key() {
            return width + "x" + height;
        }

        /**
         * Retorna o rótulo amigável da resolução para a UI.
         */
        private String label() {
            return key() + " px";
        }

        /**
         * Retorna a largura da tela em pixels CSS.
         */
        private Integer width() {
            return width;
        }

        /**
         * Retorna a altura da tela em pixels CSS.
         */
        private Integer height() {
            return height;
        }

        /**
         * Retorna a quantidade de sessões nesta resolução.
         */
        private long sessions() {
            return sessions;
        }
    }

    /**
     * Acumula tempo visível e volume de eventos por seção da landing.
     */
    private static final class SectionAccumulator {
        private final String sectionId;
        private long visibleMs;
        private long events;

        /**
         * Cria acumulador de seção para o identificador normalizado da landing.
         */
        private SectionAccumulator(String sectionId) {
            this.sectionId = sectionId;
        }

        /**
         * Soma um evento de tempo visível para a seção.
         */
        private void record(long elapsedMs) {
            events++;
            visibleMs += elapsedMs;
        }

        /**
         * Retorna o tempo visível acumulado para ordenação das seções.
         */
        private long visibleMs() {
            return visibleMs;
        }

        /**
         * Converte o acumulador de seção em DTO serializável pela API.
         */
        private ExperimentLandingAnalyticsSectionDto toDto() {
            return new ExperimentLandingAnalyticsSectionDto(sectionId, visibleMs, events);
        }
    }

    /**
     * Métrica agregada usada para transportar total, contagem única e último evento das consultas SQL.
     */
    private record AggregatedMetric(long total, Long uniqueCount, Instant lastEvent) {
    }
}
