package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.funnel.dto.RegisterExperimentFunnelEventRequest;
import com.marketinghub.experiment.funnel.ExperimentFunnelEventRepository.StageAggregation;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.model.Lead;
import com.marketinghub.repository.LeadRepository;
import com.marketinghub.leadportal.dto.RegisterLeadPortalSubmissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço que consolida as etapas do funil de vendas de um experimento.
 */
@Service
@RequiredArgsConstructor
public class ExperimentFunnelService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentFunnelEventRepository eventRepository;
    private final LeadRepository leadRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String FLOW_SCOPE_CONDITION = """
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

    public List<ExperimentFunnelStageDto> summarize(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages = bootstrapStages();

        applyManualEvents(experiment.getId(), stages);
        applyAutomaticMetrics(experiment.getId(), stages);
        fillDefaultSourceWhenMissing(stages);

        return stages.values().stream()
                .sorted(Comparator.comparingInt(ExperimentFunnelStageDto::getOrder))
                .toList();
    }

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
                .payload(request.payload())
                .occurredAt(Optional.ofNullable(request.occurredAt()).orElse(Instant.now()))
                .build();
        eventRepository.save(event);
    }

    @Transactional
    public void registerFormRenderCompleted(String flowSlug, String visitorId) {
        if (flowSlug == null || flowSlug.isBlank()) {
            throw new IllegalArgumentException("Slug do fluxo é obrigatório");
        }
        Experiment experiment = experimentRepository.findFirstByLeadPortalFlowSlug(flowSlug.trim())
                .orElseThrow(() -> new IllegalArgumentException("Fluxo não vinculado a experimento"));

        String sanitizedVisitorId = visitorId == null ? null : visitorId.trim();
        String payload = sanitizedVisitorId == null || sanitizedVisitorId.isBlank()
                ? null
                : "visitorId=" + sanitizedVisitorId;

        ExperimentFunnelEvent event = ExperimentFunnelEvent.builder()
                .experiment(experiment)
                .stage(ExperimentFunnelStage.VISUALIZACAO_FORM)
                .source(ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE)
                .payload(payload)
                .occurredAt(Instant.now())
                .build();
        eventRepository.save(event);
    }

    @Transactional
    public void registerFormSubmission(String flowSlug, RegisterLeadPortalSubmissionRequest request) {
        if (flowSlug == null || flowSlug.isBlank()) {
            throw new IllegalArgumentException("Slug do fluxo é obrigatório");
        }
        if (request == null || request.submissionId() == null || request.submissionId().isBlank()) {
            throw new IllegalArgumentException("ID da submissão é obrigatório");
        }

        Experiment experiment = experimentRepository.findFirstByLeadPortalFlowSlug(flowSlug.trim())
                .orElseThrow(() -> new IllegalArgumentException("Fluxo não vinculado a experimento"));

        String payload = "submissionId=" + request.submissionId().trim();
        Instant occurredAt = Optional.ofNullable(request.submittedAt()).orElse(Instant.now());

        ExperimentFunnelEvent event = ExperimentFunnelEvent.builder()
                .experiment(experiment)
                .stage(ExperimentFunnelStage.ENVIO_FORM)
                .source(ExperimentFunnelEventRepository.SUBMISSION_SOURCE)
                .payload(payload)
                .occurredAt(occurredAt)
                .build();
        eventRepository.save(event);
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

    private void applyManualEvents(Long experimentId, Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
        for (StageAggregation agg : eventRepository.aggregateByExperiment(
                experimentId,
                ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE)) {
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

    private void applyAutomaticMetrics(Long experimentId, Map<ExperimentFunnelStage, ExperimentFunnelStageDto> stages) {
        mergeMetric(stages, ExperimentFunnelStage.VISUALIZACAO_ANUNCIO,
                fetchSingleMetric("""
                        SELECT COALESCE(SUM(impressions), 0) AS total,
                               NULL AS unique_count,
                               MAX(updated_at) AS last_event
                        FROM experiment_campaign_metric
                        WHERE experiment_id = ?
                        """, experimentId),
                "Impressões vindas do Facebook Ads (experiment_campaign_metric)");

        mergeMetric(stages, ExperimentFunnelStage.ACESSO_FORM_LEAD,
                fetchSingleMetric("""
                        SELECT COALESCE(SUM(clicks), 0) AS total,
                               NULL AS unique_count,
                               MAX(updated_at) AS last_event
                        FROM experiment_campaign_metric
                        WHERE experiment_id = ?
                        """, experimentId),
                "Cliques do anúncio para o formulário (experiment_campaign_metric)");

        mergeMetric(stages, ExperimentFunnelStage.VISUALIZACAO_FORM,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               NULL AS unique_count,
                               MAX(occurred_at) AS last_event
                        FROM experiment_funnel_event
                        WHERE experiment_id = ?
                          AND stage = 'VISUALIZACAO_FORM'
                          AND source = ?
                        """, experimentId, ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE),
                "Renderização completa registrada pelo Lead Portal (evento lead-portal-render-complete)");

        mergeMetric(stages, ExperimentFunnelStage.ENVIO_FORM,
                fetchSingleMetric("""
                        SELECT COUNT(*) AS total,
                               COUNT(DISTINCT lead_id) AS unique_count,
                               MAX(submitted_at) AS last_event
                        FROM lead_portal_submission
                        WHERE experiment_id = ?
                        """, experimentId),
                "Envios do formulário (lead_portal_submission)");

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
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId),
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
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId),
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
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId),
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
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId),
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
                        """.formatted(FLOW_SCOPE_CONDITION), experimentId, experimentId),
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

    private record AggregatedMetric(long total, Long uniqueCount, Instant lastEvent) {
    }
}
