package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.LeadPortalExperimentMetricsDto;
import com.marketinghub.leadportal.dto.LeadPortalExperimentUserDto;
import java.sql.ResultSet;
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
        String sql = """
                SELECT e.id AS experiment_id,
                       e.name AS experiment_name,
                       e.created_at AS experiment_created_at,
                       lps.id AS submission_id,
                       lps.lead_id,
                       lps.primary_contact_name,
                       lps.primary_contact_email,
                       lps.primary_contact_phone,
                       MAX(CASE WHEN lpsa.asset_id IS NOT NULL THEN 1 ELSE 0 END) AS sent_image
                FROM experiment e
                LEFT JOIN lead_portal_submission lps ON lps.experiment_id = e.id
                LEFT JOIN lead_portal_submission_answer lpsa ON lpsa.submission_id = lps.id AND lpsa.asset_id IS NOT NULL
                GROUP BY e.id,
                         e.name,
                         e.created_at,
                         lps.id,
                         lps.lead_id,
                         lps.primary_contact_name,
                         lps.primary_contact_email,
                         lps.primary_contact_phone
                ORDER BY e.created_at DESC, lps.id
                """;

        Map<Long, ExperimentMetricsAccumulator> experiments = new LinkedHashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long experimentId = rs.getLong("experiment_id");
            ExperimentMetricsAccumulator accumulator =
                    experiments.computeIfAbsent(experimentId, id -> new ExperimentMetricsAccumulator(
                            id, rs.getString("experiment_name")));

            Long submissionId = rs.getObject("submission_id", Long.class);
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

        return experiments.values().stream().map(ExperimentMetricsAccumulator::toDto).toList();
    }

    private String buildUserKey(ResultSet rs, long submissionId) throws SQLException {
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
    }

    private String buildDisplayName(ResultSet rs, long submissionId) throws SQLException {
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
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String coalesce(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    @Getter
    private static class ExperimentMetricsAccumulator {
        private final long experimentId;
        private final String experimentName;
        private final Map<String, LeadPortalExperimentUserDto> uniqueUsers = new LinkedHashMap<>();

        ExperimentMetricsAccumulator(long experimentId, String experimentName) {
            this.experimentId = experimentId;
            this.experimentName = experimentName;
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
                    leads.size(),
                    leadsWithImage,
                    leads);
        }
    }
}
