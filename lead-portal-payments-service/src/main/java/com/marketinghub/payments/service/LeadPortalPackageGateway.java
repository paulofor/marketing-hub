package com.marketinghub.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import com.marketinghub.payments.dto.OriginalAsset;
import com.marketinghub.payments.model.FlowSubmissionImagePackageStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadPortalPackageGateway {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPackageGateway.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LeadPortalPackageGateway(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public LeadPortalPackageSummary loadPackage(long packageId) {
        String sql = """
                SELECT
                    pack.id,
                    pack.submission_id,
                    pack.status,
                    pack.prompt,
                    pack.model,
                    pack.image_total_price_usd,
                    pack.image_currency,
                    pack.created_at,
                    sub.name AS submission_name,
                    sub.email AS submission_email,
                    flow.id AS flow_id,
                    flow.slug AS flow_slug,
                    COALESCE(exp.unit_price_brl, flow_exp.unit_price_brl) AS experiment_unit_price_brl,
                    COALESCE(exp.id, flow_exp.id) AS experiment_id
                FROM flow_submission_image_package pack
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                LEFT JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                LEFT JOIN experiment flow_exp ON flow.experiment_id = flow_exp.id
                WHERE pack.id = ?
                """;
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapSummary(rs), packageId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Pacote " + packageId + " não encontrado");
        }
    }

    public List<OriginalAsset> listOriginalAssets(long packageId) {
        String sql = """
                SELECT items.id, items.position_index, a.url, a.payload, a.provider
                FROM flow_submission_image_item items
                JOIN asset a ON a.id = items.asset_id
                WHERE items.package_id = ?
                ORDER BY items.position_index ASC, items.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAsset(rs), packageId);
    }

    private LeadPortalPackageSummary mapSummary(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String submissionRaw = rs.getString("submission_id");
        UUID submissionId = parseUuid(submissionRaw).orElse(null);
        FlowSubmissionImagePackageStatus status = parseStatus(rs.getString("status"));
        BigDecimal totalPrice = rs.getBigDecimal("image_total_price_usd");
        String currency = rs.getString("image_currency");
        Instant createdAt = toInstant(rs.getTimestamp("created_at"));
        Long flowId = getLong(rs, "flow_id");
        String flowSlug = rs.getString("flow_slug");
        Long experimentId = getLong(rs, "experiment_id");
        BigDecimal experimentUnitPrice = rs.getBigDecimal("experiment_unit_price_brl");
        return new LeadPortalPackageSummary(
                id,
                submissionId,
                rs.getString("submission_name"),
                rs.getString("submission_email"),
                status,
                rs.getString("prompt"),
                rs.getString("model"),
                totalPrice,
                currency,
                createdAt,
                flowId,
                flowSlug,
                experimentId,
                experimentUnitPrice);
    }

    private OriginalAsset mapAsset(ResultSet rs) throws SQLException {
        long itemId = rs.getLong("id");
        Integer position = (Integer) rs.getObject("position_index");
        String objectKey = rs.getString("url");
        String contentType = extractContentType(rs.getString("payload"));
        return new OriginalAsset(itemId, position, objectKey, contentType);
    }

    private String extractContentType(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode contentTypeNode = node.get("contentType");
            if (contentTypeNode != null && contentTypeNode.isTextual()) {
                return contentTypeNode.asText();
            }
        } catch (IOException ex) {
            log.debug("Não foi possível parsear payload do asset", ex);
        }
        return null;
    }

    private Optional<UUID> parseUuid(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Long getLong(ResultSet rs, String columnLabel) throws SQLException {
        long value = rs.getLong(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private FlowSubmissionImagePackageStatus parseStatus(String value) {
        try {
            return FlowSubmissionImagePackageStatus.valueOf(value);
        } catch (Exception ex) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
