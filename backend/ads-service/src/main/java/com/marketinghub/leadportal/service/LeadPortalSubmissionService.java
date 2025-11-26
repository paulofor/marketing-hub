package com.marketinghub.leadportal.service;

import com.marketinghub.imagedeliverable.ImageDeliverableStatus;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Consulta pacotes de imagem do Lead Portal que aguardam processamento.
 */
@Service
public class LeadPortalSubmissionService {

    private final JdbcTemplate jdbcTemplate;

    public LeadPortalSubmissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LeadPortalSubmissionDto> listPendingPackages() {
        String sql = """
                SELECT
                    pack.id AS package_id,
                    pack.lead_id,
                    flow.slug AS flow_slug,
                    lps.primary_contact_name,
                    lps.primary_contact_email,
                    lps.primary_contact_phone,
                    pack.prompt,
                    pack.status,
                    pack.created_at
                FROM image_deliverable_package pack
                LEFT JOIN (
                    SELECT latest.lead_id,
                           lps.flow_id,
                           lps.primary_contact_name,
                           lps.primary_contact_email,
                           lps.primary_contact_phone
                    FROM lead_portal_submission lps
                    JOIN (
                        SELECT lead_id, MAX(submitted_at) AS submitted_at
                        FROM lead_portal_submission
                        GROUP BY lead_id
                    ) latest ON latest.lead_id = lps.lead_id AND latest.submitted_at = lps.submitted_at
                ) lps ON lps.lead_id = pack.lead_id
                LEFT JOIN lead_portal_flow flow ON flow.id = lps.flow_id
                WHERE pack.status = 'RECEIVED'
                ORDER BY pack.created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPackage(rs));
    }

    private LeadPortalSubmissionDto mapPackage(ResultSet rs) throws SQLException {
        String status = rs.getString("status");

        return new LeadPortalSubmissionDto(
                rs.getLong("package_id"),
                rs.getObject("lead_id", UUID.class),
                rs.getString("flow_slug"),
                rs.getString("primary_contact_name"),
                rs.getString("primary_contact_email"),
                rs.getString("primary_contact_phone"),
                rs.getString("prompt"),
                ImageDeliverableStatus.valueOf(status),
                rs.getTimestamp("created_at").toInstant());
    }
}
