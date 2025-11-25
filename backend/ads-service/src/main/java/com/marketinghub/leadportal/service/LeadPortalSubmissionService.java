package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.LeadPortalSubmissionDto;
import com.marketinghub.leadportal.integration.LeadPortalIntegrationProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Consulta envios de imagem feitos no Lead Portal.
 */
@Service
public class LeadPortalSubmissionService {

    private final JdbcTemplate jdbcTemplate;
    private final LeadPortalIntegrationProperties integrationProperties;

    public LeadPortalSubmissionService(
            JdbcTemplate jdbcTemplate, LeadPortalIntegrationProperties integrationProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.integrationProperties = integrationProperties;
    }

    public List<LeadPortalSubmissionDto> listWithImages() {
        String sql = """
                SELECT id, flow_slug, name, email, stored_file_name, created_at
                FROM flow_submissions
                WHERE stored_file_name IS NOT NULL
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapSubmission(rs));
    }

    private LeadPortalSubmissionDto mapSubmission(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String storedFileName = rs.getString("stored_file_name");

        return new LeadPortalSubmissionDto(
                id,
                rs.getString("flow_slug"),
                rs.getString("name"),
                rs.getString("email"),
                buildImageUrl(id, storedFileName),
                rs.getTimestamp("created_at").toInstant());
    }

    private String buildImageUrl(UUID id, String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            return null;
        }

        String baseUrl = integrationProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }

        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return normalizedBase + "/api/flows/submissions/" + id + "/image";
    }
}
