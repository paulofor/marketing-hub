package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

/**
 * Consolida as conversões por anúncio a partir dos códigos de campanha usados no Lead Portal.
 */
@Service
@RequiredArgsConstructor
public class ExperimentFunnelAttributionService {

    private final ExperimentRepository experimentRepository;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, EnumMap<ExperimentFunnelStage, Long>> aggregateByCampaignCode(Long experimentId) {
        if (experimentId == null) {
            return Map.of();
        }
        Experiment experiment = experimentRepository.findById(experimentId).orElse(null);
        if (experiment == null) {
            return Map.of();
        }
        Instant baseline = resolveBaseline(experiment);
        Map<String, EnumMap<ExperimentFunnelStage, Long>> result = new HashMap<>();
        collectRenderEvents(experimentId, baseline, result);
        collectSubmissions(experimentId, baseline, result);
        collectSampleEmailOpens(experimentId, baseline, result);
        collectCheckoutAccess(experimentId, baseline, result);
        collectPurchases(experimentId, baseline, result);
        collectDeliveryEmailOpens(experimentId, baseline, result);
        collectDownloads(experimentId, baseline, result);
        return result;
    }

    private void collectRenderEvents(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT campaign_code, COUNT(*) AS total
                FROM experiment_funnel_event
                WHERE experiment_id = ?
                  AND stage = 'VISUALIZACAO_FORM'
                  AND source = ?
                  AND campaign_code IS NOT NULL
                  AND (? IS NULL OR occurred_at > ?)
                GROUP BY campaign_code
                """;
        queryByCampaignCode(sql, ExperimentFunnelStage.VISUALIZACAO_FORM, target,
                experimentId, ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE, baseline, baseline);
    }

    private void collectSubmissions(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT s.campaign_code, COUNT(*) AS total
                FROM flow_submissions s
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE s.campaign_code IS NOT NULL
                  AND %s
                  AND (? IS NULL OR s.created_at > ?)
                GROUP BY s.campaign_code
                """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION);
        queryByCampaignCode(sql, ExperimentFunnelStage.ENVIO_FORM, target,
                experimentId, experimentId, baseline, baseline);
    }

    private void collectSampleEmailOpens(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT s.campaign_code, COUNT(*) AS total
                FROM flow_submission_image_package p
                JOIN flow_submissions s ON s.id = p.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE s.campaign_code IS NOT NULL
                  AND %s
                  AND p.email_opened_at IS NOT NULL
                  AND (? IS NULL OR p.email_opened_at > ?)
                GROUP BY s.campaign_code
                """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION);
        queryByCampaignCode(sql, ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA, target,
                experimentId, experimentId, baseline, baseline);
    }

    private void collectCheckoutAccess(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT s.campaign_code, COUNT(*) AS total
                FROM lead_portal_purchase p
                JOIN flow_submissions s ON s.id = p.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE s.campaign_code IS NOT NULL
                  AND %s
                  AND p.checkout_accessed_at IS NOT NULL
                  AND (? IS NULL OR p.checkout_accessed_at > ?)
                GROUP BY s.campaign_code
                """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION);
        queryByCampaignCode(sql, ExperimentFunnelStage.ACESSO_CHECKOUT, target,
                experimentId, experimentId, baseline, baseline);
    }

    private void collectPurchases(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT s.campaign_code, COUNT(*) AS total
                FROM lead_portal_purchase p
                JOIN flow_submissions s ON s.id = p.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE s.campaign_code IS NOT NULL
                  AND %s
                  AND (p.payment_approved_at IS NOT NULL OR p.mp_status = 'approved')
                  AND (? IS NULL OR COALESCE(p.payment_approved_at, p.updated_at) > ?)
                GROUP BY s.campaign_code
                """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION);
        queryByCampaignCode(sql, ExperimentFunnelStage.COMPRA, target,
                experimentId, experimentId, baseline, baseline);
    }

    private void collectDeliveryEmailOpens(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT s.campaign_code, COUNT(*) AS total
                FROM lead_portal_premium_delivery d
                JOIN flow_submissions s ON s.id = d.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                LEFT JOIN email_log el ON el.request_id = d.email_request_id
                WHERE s.campaign_code IS NOT NULL
                  AND %s
                  AND d.email_request_id IS NOT NULL
                  AND el.opened_at IS NOT NULL
                  AND (? IS NULL OR el.opened_at > ?)
                GROUP BY s.campaign_code
                """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION);
        queryByCampaignCode(sql, ExperimentFunnelStage.ABERTURA_EMAIL_COMPRA, target,
                experimentId, experimentId, baseline, baseline);
    }

    private void collectDownloads(Long experimentId, Instant baseline, Map<String, EnumMap<ExperimentFunnelStage, Long>> target) {
        String sql = """
                SELECT s.campaign_code, COUNT(*) AS total
                FROM flow_submission_image_package p
                JOIN flow_submissions s ON s.id = p.submission_id
                JOIN lead_portal_flow f ON f.slug = s.flow_slug
                WHERE s.campaign_code IS NOT NULL
                  AND %s
                  AND p.payment_purchase_id IS NOT NULL
                  AND p.images_viewed_at IS NOT NULL
                  AND (? IS NULL OR p.images_viewed_at > ?)
                GROUP BY s.campaign_code
                """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION);
        queryByCampaignCode(sql, ExperimentFunnelStage.DOWNLOAD_MATERIAL_PAGO, target,
                experimentId, experimentId, baseline, baseline);
    }

    private void queryByCampaignCode(String sql, ExperimentFunnelStage stage,
                                     Map<String, EnumMap<ExperimentFunnelStage, Long>> target,
                                     Object... params) {
        jdbcTemplate.query(sql, (RowCallbackHandler) rs ->
                record(target, rs.getString("campaign_code"), stage, rs.getLong("total")), params);
    }

    private void record(Map<String, EnumMap<ExperimentFunnelStage, Long>> target,
                        String rawCode,
                        ExperimentFunnelStage stage,
                        long amount) {
        if (amount <= 0) {
            return;
        }
        String normalized = normalizeCode(rawCode);
        if (normalized == null) {
            return;
        }
        EnumMap<ExperimentFunnelStage, Long> stageMap = target.computeIfAbsent(normalized,
                key -> new EnumMap<>(ExperimentFunnelStage.class));
        stageMap.merge(stage, amount, Long::sum);
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private Instant resolveBaseline(Experiment experiment) {
        Instant release = experiment.getFacebookReleaseRequestedAt();
        Instant reset = experiment.getFunnelResetAt();
        if (release == null) {
            return reset;
        }
        if (reset == null) {
            return release;
        }
        return release.isAfter(reset) ? release : reset;
    }
}
