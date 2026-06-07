package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.RegisterExperimentFunnelEventRequest;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDeviceDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSectionDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSessionDto;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionEngagementContractV1;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import com.marketinghub.leadportal.dto.RegisterLeadPortalSubmissionRequest;
import com.marketinghub.model.Lead;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository.StageAggregation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
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
            long elapsedMs = parseLong(payload.get("elapsedMs"));

            LandingAnalyticsSessionAccumulator session = sessions.computeIfAbsent(
                    sessionId, LandingAnalyticsSessionAccumulator::new);
            session.record(row.occurredAt(), eventType, sectionId, elapsedMs, pageUrl, userAgent, deviceType);

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
        return new ExperimentLandingAnalyticsDto(
                rows.size(),
                sessions.size(),
                pageViews,
                sectionViewEvents,
                totalVisibleMs,
                averageVisibleMsPerSession,
                lastEventAt,
                deviceBreakdown,
                sessionDtos);
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
        int deletedLandingAnalytics = eventRepository.deleteByExperimentIdAndSource(
                experimentId,
                ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
        int deletedRemainingEvents = eventRepository.deleteByExperimentId(experimentId);
        Instant now = Instant.now();
        experiment.setFunnelResetAt(now);
        experimentRepository.save(experiment);
        log.info(
                "experiment_funnel_reset experimentId={} deletedLandingAnalytics={} deletedRemainingEvents={} resetAt={}",
                experimentId,
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
     * Registra analytics da landing publicada e converte eventos compatíveis em visualização do formulário.
     */
    @Transactional
    public void registerLandingPageAnalyticsEvent(String flowSlug, RegisterLandingPageAnalyticsEventRequest request) {
        if (flowSlug == null || flowSlug.isBlank()) {
            throw new IllegalArgumentException("Slug do fluxo é obrigatório");
        }
        if (request == null || request.eventId() == null || request.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId é obrigatório");
        }
        if (request.eventType() == null || request.eventType().isBlank()) {
            throw new IllegalArgumentException("eventType é obrigatório");
        }
        Experiment experiment = resolveExperimentByFlowSlug(flowSlug.trim());

        Long elapsedMs = request.elapsedMs() != null ? request.elapsedMs() : request.visibleMs();
        String eventType = request.eventType().trim();
        Instant occurredAt = Optional.ofNullable(request.occurredAt()).orElse(Instant.now());
        String payload = buildLandingAnalyticsPayload(request, elapsedMs);

        log.info("landing_page_analytics experimentId={} flowSlug={} eventId={} eventType={} sectionId={} elapsedMs={} sessionId={} pageUrl={} occurredAt={} userAgent={} deviceType={}",
                experiment.getId(),
                flowSlug.trim(),
                request.eventId().trim(),
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
            eventRepository.save(event);
        }
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
        String sessionId = sanitizePayloadValue(request.sessionId());
        String url = sanitizePayloadValue(request.pageUrl());
        String userAgent = sanitizePayloadValue(request.userAgent());
        String deviceType = sanitizePayloadValue(
                normalizeLandingAnalyticsDeviceType(request.deviceType(), request.userAgent()));
        String duration = elapsedMs == null ? "" : elapsedMs.toString();
        return "eventId=" + sanitizePayloadValue(request.eventId())
                + ";eventType=" + sanitizePayloadValue(request.eventType())
                + ";sessionId=" + sessionId
                + ";sectionId=" + sectionId
                + ";elapsedMs=" + duration
                + ";pageUrl=" + url
                + ";userAgent=" + userAgent
                + ";deviceType=" + deviceType;
    }

    private Lead resolveLead(UUID leadId) {
        if (leadId == null) {
            return null;
        }
        return leadRepository.findById(leadId).orElse(null);
    }

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

    private Instant resolveBaseline(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return max(experiment.getFacebookReleaseRequestedAt(), experiment.getFunnelResetAt());
    }

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

    private void fillDefaultSourceWhenMissing(Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
        stages.values().forEach(dto -> {
            if (dto.getSource() == null && dto.getManualCount() > 0) {
                dto.setSource("Eventos manuais registrados na aplicação");
            }
        });
    }

    private Long sum(Long a, Long b) {
        if (a == null && b == null) return null;
        return Optional.ofNullable(a).orElse(0L) + Optional.ofNullable(b).orElse(0L);
    }

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
        private final Map<String, SectionAccumulator> sections = new LinkedHashMap<>();

        private LandingAnalyticsSessionAccumulator(String sessionId) {
            this.sessionId = sessionId;
        }

        /**
         * Acrescenta um evento recebido na sessão e atualiza contadores de página e seção.
         */
        private void record(Instant occurredAt, String eventType, String sectionId, long elapsedMs,
                            String pageUrl, String userAgent, String deviceType) {
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
                    topSections);
        }
    }

    /**
     * Acumula tempo visível e volume de eventos por seção da landing.
     */
    private static final class SectionAccumulator {
        private final String sectionId;
        private long visibleMs;
        private long events;

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

    private record AggregatedMetric(long total, Long uniqueCount, Instant lastEvent) {
    }
}
