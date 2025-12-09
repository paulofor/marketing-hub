package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.LeadPortalExperimentMetricsDto;
import com.marketinghub.leadportal.dto.LeadPortalExperimentUserDto;
import java.sql.ResultSet;
import java.time.Instant;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Consulta métricas consolidadas do portal do lead.
 */
@Service
@RequiredArgsConstructor
public class LeadPortalMetricsService {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Retorna contagens básicas de submissões e envios de imagem por experimento.
     */
    public List<LeadPortalExperimentMetricsDto> listExperimentMetrics() {
        Map<Long, ExperimentMetricsAccumulator> experiments = new LinkedHashMap<>();

        populateAccessCounts(experiments);
        populateSubmissionMetrics(experiments);
        populateSampleEmailMetrics(experiments);
        populatePackageMetrics(experiments);

        return experiments.values().stream().map(ExperimentMetricsAccumulator::toDto).toList();
    }

    private void populateAccessCounts(Map<Long, ExperimentMetricsAccumulator> experiments) {
        String accessSql = """
                SELECT e.id AS experiment_id,
                       e.name AS experiment_name,
                       COUNT(DISTINCT COALESCE(fa.visitor_id,
                                                CONCAT('ip:', fa.client_ip),
                                                CONCAT('access:', fa.id))) AS unique_accesses
                FROM experiment e
                JOIN lead_portal_flow lpf ON lpf.id = e.lead_portal_flow_id
                LEFT JOIN flow_access fa ON fa.flow_slug = lpf.slug
                GROUP BY e.id, e.name
                ORDER BY e.created_at DESC
                """;

        jdbcTemplate.query(accessSql, rs -> {
            Long experimentId = rs.getLong("experiment_id");
            String experimentName = getString(rs, "experiment_name");
            ExperimentMetricsAccumulator accumulator =
                    experiments.computeIfAbsent(
                            experimentId, id -> new ExperimentMetricsAccumulator(id, experimentName));

            accumulator.setLeadsAccessed(rs.getLong("unique_accesses"));
        });
    }

    private void populateSubmissionMetrics(Map<Long, ExperimentMetricsAccumulator> experiments) {
        String submissionSql = """
                SELECT e.id AS experiment_id,
                       e.name AS experiment_name,
                       submissions.submission_id,
                       submissions.lead_id,
                       submissions.primary_contact_name,
                       submissions.primary_contact_email,
                       submissions.primary_contact_phone,
                       submissions.sent_image
                FROM experiment e
                LEFT JOIN (
                    SELECT lps.experiment_id,
                           lps.id AS submission_id,
                           lps.lead_id,
                           lps.primary_contact_name,
                           lps.primary_contact_email,
                           lps.primary_contact_phone,
                           MAX(CASE WHEN lpsa.asset_id IS NOT NULL THEN 1 ELSE 0 END) AS sent_image
                    FROM lead_portal_submission lps
                    LEFT JOIN lead_portal_submission_answer lpsa
                        ON lpsa.submission_id = lps.id AND lpsa.asset_id IS NOT NULL
                    GROUP BY lps.experiment_id,
                             lps.id,
                             lps.lead_id,
                             lps.primary_contact_name,
                             lps.primary_contact_email,
                             lps.primary_contact_phone
                    UNION ALL
                    SELECT exp.id AS experiment_id,
                           fs.id AS submission_id,
                           NULL AS lead_id,
                           fs.name AS primary_contact_name,
                           fs.email AS primary_contact_email,
                           NULL AS primary_contact_phone,
                           CASE WHEN fs.stored_file_name IS NOT NULL THEN 1 ELSE 0 END AS sent_image
                    FROM flow_submissions fs
                    JOIN lead_portal_flow lpf ON lpf.slug = fs.flow_slug
                    JOIN experiment exp ON exp.lead_portal_flow_id = lpf.id
                ) submissions ON submissions.experiment_id = e.id
                ORDER BY e.created_at DESC, submissions.submission_id
                """;

        jdbcTemplate.query(submissionSql, rs -> {
            Long experimentId = rs.getLong("experiment_id");
            String experimentName = getString(rs, "experiment_name");
            ExperimentMetricsAccumulator accumulator =
                    experiments.computeIfAbsent(
                            experimentId, id -> new ExperimentMetricsAccumulator(id, experimentName));

            String submissionId = getString(rs, "submission_id");
            if (submissionId == null) {
                return;
            }

            String userKey = buildUserKey(rs, submissionId);
            LeadPortalExperimentUserDto user = new LeadPortalExperimentUserDto(
                    buildDisplayName(rs, submissionId),
                    normalize(rs.getString("primary_contact_email")),
                    normalize(rs.getString("primary_contact_phone")),
                    rs.getBoolean("sent_image"));

            accumulator.addUser(userKey, user);
        });
    }

    private void populateSampleEmailMetrics(Map<Long, ExperimentMetricsAccumulator> experiments) {
        String sql = """
                SELECT exp.id AS experiment_id,
                       exp.name AS experiment_name,
                       COUNT(sample.id) AS sample_count,
                       sel.id AS selected_id,
                       sel.subject AS selected_subject,
                       sel.preview_text AS selected_preview,
                       sel.call_to_action AS selected_cta,
                       sel.updated_at AS selected_updated_at
                FROM experiment exp
                LEFT JOIN experiment_sample_email sample ON sample.experiment_id = exp.id
                LEFT JOIN experiment_sample_email sel ON sel.id = exp.selected_sample_email_id
                GROUP BY exp.id, exp.name, sel.id, sel.subject, sel.preview_text, sel.call_to_action, sel.updated_at
                """;

        jdbcTemplate.query(sql, rs -> {
            Long experimentId = rs.getLong("experiment_id");
            String experimentName = getString(rs, "experiment_name");
            ExperimentMetricsAccumulator accumulator = experiments.computeIfAbsent(
                    experimentId, id -> new ExperimentMetricsAccumulator(id, experimentName));

            accumulator.setSampleEmailCount(rs.getLong("sample_count"));
            Long selectedId = getLong(rs, "selected_id");
            accumulator.setSelectedSampleEmail(
                    selectedId,
                    getString(rs, "selected_subject"),
                    getString(rs, "selected_preview"),
                    getString(rs, "selected_cta"),
                    getInstant(rs, "selected_updated_at"));
        });
    }

    private void populatePackageMetrics(Map<Long, ExperimentMetricsAccumulator> experiments) {
        String sql = """
                SELECT exp.id AS experiment_id,
                       exp.name AS experiment_name,
                       SUM(CASE WHEN items.item_count IS NOT NULL AND items.item_count > 0
                                AND watermarks.watermark_count >= items.item_count THEN 1 ELSE 0 END) AS packages_with_watermark,
                       SUM(CASE WHEN pack.notified_at IS NOT NULL THEN 1 ELSE 0 END) AS packages_notified,
                       MAX(pack.notified_at) AS last_notified_at
                FROM experiment exp
                LEFT JOIN lead_portal_flow flow ON flow.id = exp.lead_portal_flow_id
                LEFT JOIN flow_submissions sub ON sub.flow_slug = flow.slug
                LEFT JOIN flow_submission_image_package pack ON pack.submission_id = sub.id
                LEFT JOIN (
                    SELECT item.package_id, COUNT(*) AS item_count
                    FROM flow_submission_image_item item
                    GROUP BY item.package_id
                ) items ON items.package_id = pack.id
                LEFT JOIN (
                    SELECT item.package_id, COUNT(wm.id) AS watermark_count
                    FROM flow_submission_image_item item
                    LEFT JOIN flow_submission_image_watermark wm ON wm.item_id = item.id
                    GROUP BY item.package_id
                ) watermarks ON watermarks.package_id = pack.id
                GROUP BY exp.id, exp.name
                """;

        jdbcTemplate.query(sql, rs -> {
            Long experimentId = rs.getLong("experiment_id");
            String experimentName = getString(rs, "experiment_name");
            ExperimentMetricsAccumulator accumulator = experiments.computeIfAbsent(
                    experimentId, id -> new ExperimentMetricsAccumulator(id, experimentName));

            accumulator.setPackagesWithWatermark(getLong(rs, "packages_with_watermark", 0L));
            accumulator.setPackagesNotified(getLong(rs, "packages_notified", 0L));
            accumulator.updateLastPackageNotificationAt(getInstant(rs, "last_notified_at"));
        });
    }

    private String buildUserKey(ResultSet rs, String submissionId) {
        try {
            byte[] leadIdBytes = rs.getBytes("lead_id");
            String email = normalize(rs.getString("primary_contact_email"));
            String phone = normalize(rs.getString("primary_contact_phone"));

            if (leadIdBytes != null && leadIdBytes.length > 0) {
                return "lead:" + Base64.getEncoder().encodeToString(leadIdBytes);
            }
            if (email != null) {
                return "email:" + email.toLowerCase(Locale.ROOT);
            }
            if (phone != null) {
                return "phone:" + phone;
            }
            return "submission:" + submissionId;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao ler métricas do portal do lead", ex);
        }
    }

    private String buildDisplayName(ResultSet rs, String submissionId) {
        try {
            String name = normalize(rs.getString("primary_contact_name"));
            String email = normalize(rs.getString("primary_contact_email"));
            String phone = normalize(rs.getString("primary_contact_phone"));
            byte[] leadIdBytes = rs.getBytes("lead_id");

            if (name != null) {
                return name;
            }
            if (email != null) {
                return email;
            }
            if (phone != null) {
                return phone;
            }
            if (leadIdBytes != null && leadIdBytes.length > 0) {
                return "Lead " + Base64.getEncoder().encodeToString(leadIdBytes).substring(0, 8);
            }
            return "Submissão #" + submissionId;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao ler métricas do portal do lead", ex);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getString(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao ler métricas do portal do lead", ex);
        }
    }

    private Long getLong(ResultSet rs, String columnLabel) {
        try {
            long value = rs.getLong(columnLabel);
            return rs.wasNull() ? null : value;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao ler métricas do portal do lead", ex);
        }
    }

    private long getLong(ResultSet rs, String columnLabel, long defaultValue) {
        try {
            long value = rs.getLong(columnLabel);
            return rs.wasNull() ? defaultValue : value;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao ler métricas do portal do lead", ex);
        }
    }

    private java.time.Instant getInstant(ResultSet rs, String columnLabel) {
        try {
            java.sql.Timestamp ts = rs.getTimestamp(columnLabel);
            return ts != null ? ts.toInstant() : null;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao ler métricas do portal do lead", ex);
        }
    }

    private static String coalesce(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    @Getter
    private static class ExperimentMetricsAccumulator {
        private final long experimentId;
        private final String experimentName;
        private long leadsAccessed;
        private final Map<String, LeadPortalExperimentUserDto> uniqueUsers = new LinkedHashMap<>();
        private long sampleEmailCount;
        private Long selectedSampleEmailId;
        private String selectedSampleEmailSubject;
        private String selectedSampleEmailPreviewText;
        private String selectedSampleEmailCallToAction;
        private Instant selectedSampleEmailUpdatedAt;
        private long packagesWithWatermark;
        private long packagesNotified;
        private Instant lastPackageNotificationAt;

        ExperimentMetricsAccumulator(long experimentId, String experimentName) {
            this.experimentId = experimentId;
            this.experimentName = experimentName;
        }

        void setLeadsAccessed(long leadsAccessed) {
            this.leadsAccessed = leadsAccessed;
        }

        void setSampleEmailCount(long sampleEmailCount) {
            this.sampleEmailCount = sampleEmailCount;
        }

        void setSelectedSampleEmail(Long id, String subject, String preview, String cta, Instant updatedAt) {
            this.selectedSampleEmailId = id;
            this.selectedSampleEmailSubject = subject;
            this.selectedSampleEmailPreviewText = preview;
            this.selectedSampleEmailCallToAction = cta;
            this.selectedSampleEmailUpdatedAt = updatedAt;
        }

        void setPackagesWithWatermark(long packagesWithWatermark) {
            this.packagesWithWatermark = packagesWithWatermark;
        }

        void setPackagesNotified(long packagesNotified) {
            this.packagesNotified = packagesNotified;
        }

        void updateLastPackageNotificationAt(Instant instant) {
            if (instant != null && (this.lastPackageNotificationAt == null || this.lastPackageNotificationAt.isBefore(instant))) {
                this.lastPackageNotificationAt = instant;
            }
        }

        void addUser(String userKey, LeadPortalExperimentUserDto user) {
            uniqueUsers.merge(userKey, user, (existing, incoming) -> new LeadPortalExperimentUserDto(
                    coalesce(existing.displayName(), incoming.displayName()),
                    coalesce(existing.email(), incoming.email()),
                    coalesce(existing.phone(), incoming.phone()),
                    existing.sentImage() || incoming.sentImage()));
        }

        LeadPortalExperimentMetricsDto toDto() {
            List<LeadPortalExperimentUserDto> leads = new ArrayList<>(uniqueUsers.values());
            long leadsWithImage = leads.stream().filter(LeadPortalExperimentUserDto::sentImage).count();

            return new LeadPortalExperimentMetricsDto(
                    experimentId,
                    experimentName,
                    leadsAccessed,
                    leadsWithImage,
                    leads,
                    sampleEmailCount,
                    selectedSampleEmailId,
                    selectedSampleEmailSubject,
                    selectedSampleEmailPreviewText,
                    selectedSampleEmailCallToAction,
                    selectedSampleEmailUpdatedAt,
                    packagesWithWatermark,
                    packagesNotified,
                    lastPackageNotificationAt);
        }
    }
}
