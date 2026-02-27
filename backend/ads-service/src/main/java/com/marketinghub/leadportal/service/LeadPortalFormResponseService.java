package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.dto.LeadPortalFormResponseAnswerDto;
import com.marketinghub.leadportal.dto.LeadPortalFormResponseDto;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Provides read access to the most recent Lead Portal form submissions. */
@Service
public class LeadPortalFormResponseService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalFormResponseService.class);
    private static final int MAX_LIMIT = 200;
    private static final List<String> PHONE_KEYS =
            List.of("phone", "telefone", "whatsapp", "celular", "primary_contact_phone", "phone_number");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LeadPortalFormResponseService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<LeadPortalFormResponseDto> listRecentResponses(int limit) {
        int sanitizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String sql = """
                SELECT
                    sub.id,
                    sub.flow_slug,
                    flow.name AS flow_name,
                    exp.id AS experiment_id,
                    exp.name AS experiment_name,
                    sub.name AS lead_name,
                    sub.email AS lead_email,
                    sub.answers AS answers_json,
                    sub.created_at
                FROM flow_submissions sub
                LEFT JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                ORDER BY sub.created_at DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, sanitizedLimit);
            return ps;
        }, this::mapResponse);
    }

    private LeadPortalFormResponseDto mapResponse(ResultSet rs, int rowNum) throws SQLException {
        String answersJson = rs.getString("answers_json");
        List<LeadPortalFormResponseAnswerDto> answers = parseAnswers(answersJson);
        return new LeadPortalFormResponseDto(
                mapSubmissionId(rs),
                rs.getString("flow_slug"),
                rs.getString("flow_name"),
                getLong(rs, "experiment_id"),
                rs.getString("experiment_name"),
                rs.getString("lead_name"),
                rs.getString("lead_email"),
                extractPhone(answers),
                getInstant(rs, "created_at"),
                answers);
    }

    private UUID mapSubmissionId(ResultSet rs) throws SQLException {
        Object rawValue = rs.getObject("id");
        if (rawValue instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }

        String submission = rs.getString("id");
        if (!StringUtils.hasText(submission)) {
            return null;
        }

        try {
            return UUID.fromString(submission);
        } catch (IllegalArgumentException ex) {
            log.warn("Valor de submission_id '{}' não pôde ser convertido para UUID", submission);
            return null;
        }
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private List<LeadPortalFormResponseAnswerDto> parseAnswers(String answersJson) {
        if (!StringUtils.hasText(answersJson)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(answersJson);
            List<LeadPortalFormResponseAnswerDto> answers = new ArrayList<>();
            if (root.isObject()) {
                root.fields().forEachRemaining(field ->
                        answers.add(new LeadPortalFormResponseAnswerDto(field.getKey(), formatValue(field.getValue()))));
            } else if (root.isArray()) {
                int index = 0;
                for (JsonNode node : root) {
                    answers.add(new LeadPortalFormResponseAnswerDto("item_" + index++, formatValue(node)));
                }
            } else {
                answers.add(new LeadPortalFormResponseAnswerDto("valor", formatValue(root)));
            }
            return List.copyOf(answers);
        } catch (JsonProcessingException ex) {
            log.warn("Não foi possível interpretar respostas do formulário", ex);
            return List.of(new LeadPortalFormResponseAnswerDto("raw", answersJson));
        }
    }

    private String formatValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isValueNode()) {
            return node.asText().trim();
        }
        return node.toString();
    }

    private String extractPhone(List<LeadPortalFormResponseAnswerDto> answers) {
        for (String candidate : PHONE_KEYS) {
            for (LeadPortalFormResponseAnswerDto answer : answers) {
                if (answer.key() != null && answer.key().equalsIgnoreCase(candidate)) {
                    String value = answer.value();
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }
}
