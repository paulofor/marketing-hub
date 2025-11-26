package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionDto;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Consulta pacotes de imagem do Lead Portal que aguardam processamento.
 */
@Service
public class LeadPortalSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalSubmissionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LeadPortalSubmissionService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<LeadPortalSubmissionDto> listPendingPackages() {
        String sql = """
                SELECT
                    pack.id AS package_id,
                    pack.submission_id,
                    sub.flow_slug,
                    sub.name AS submission_name,
                    sub.email AS submission_email,
                    sub.answers,
                    pack.prompt,
                    pack.status,
                    pack.created_at
                FROM flow_submission_image_package pack
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                WHERE pack.status IN ('RECENT', 'RECEIVED')
                ORDER BY pack.created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapPackage(rs));
    }

    private LeadPortalSubmissionDto mapPackage(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return new LeadPortalSubmissionDto(
                rs.getLong("package_id"),
                mapSubmissionId(rs),
                rs.getString("flow_slug"),
                rs.getString("submission_name"),
                rs.getString("submission_email"),
                extractPhone(rs.getString("answers")),
                rs.getString("prompt"),
                mapStatus(status),
                rs.getTimestamp("created_at").toInstant());
    }

    private FlowSubmissionImagePackageStatus mapStatus(String status) {
        try {
            return FlowSubmissionImagePackageStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            log.warn("Lead Portal image package with unknown status '{}'", status);
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private UUID mapSubmissionId(ResultSet rs) throws SQLException {
        Object rawValue = rs.getObject("submission_id");

        if (rawValue instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }

        return UUID.fromString(rs.getString("submission_id"));
    }

    private String extractPhone(String answersJson) {
        if (answersJson == null || answersJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(answersJson);
            for (String key : List.of("phone", "telefone", "whatsapp")) {
                JsonNode node = root.get(key);
                if (node != null && node.isValueNode()) {
                    String value = node.asText().trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse flow submission answers for phone extraction", e);
        }

        return null;
    }
}
