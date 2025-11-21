package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.LeadPortalExperimentMetricsDto;
import java.util.List;
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
                       COUNT(DISTINCT lps.lead_id) AS leads_accessed,
                       COUNT(DISTINCT CASE WHEN lpsa.asset_id IS NOT NULL THEN lps.lead_id END) AS leads_with_image
                FROM experiment e
                LEFT JOIN lead_portal_submission lps ON lps.experiment_id = e.id
                LEFT JOIN lead_portal_submission_answer lpsa ON lpsa.submission_id = lps.id AND lpsa.asset_id IS NOT NULL
                GROUP BY e.id, e.name
                ORDER BY e.created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new LeadPortalExperimentMetricsDto(
                        rs.getLong("experiment_id"),
                        rs.getString("experiment_name"),
                        rs.getLong("leads_accessed"),
                        rs.getLong("leads_with_image")));
    }
}
