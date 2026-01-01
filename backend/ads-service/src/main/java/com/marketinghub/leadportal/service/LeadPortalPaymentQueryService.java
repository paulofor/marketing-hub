package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.dto.LeadPortalPaymentDto;
import com.marketinghub.leadportal.dto.LeadPortalPaymentHistoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class LeadPortalPaymentQueryService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPaymentQueryService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LeadPortalPaymentQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<LeadPortalPaymentDto> listRecentPayments(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<LeadPortalPaymentRow> rows = jdbcTemplate.query(
                "SELECT id, package_id, submission_id, buyer_name, buyer_email, status, mp_payment_id, mp_preference_id, "
                        + "mp_status, mp_payment_payload, amount, currency, checkout_expires_at, payment_approved_at, delivered_at, created_at, updated_at "
                        + "FROM lead_portal_purchase ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> mapPurchase(rs),
                safeLimit
        );

        Map<String, List<LeadPortalPaymentHistoryDto>> webhookHistory = fetchWebhookHistory(rows);

        return rows.stream()
                .map(row -> toDto(row, webhookHistory.get(row.mercadoPagoPaymentId())))
                .toList();
    }

    private LeadPortalPaymentDto toDto(LeadPortalPaymentRow row, List<LeadPortalPaymentHistoryDto> webhookHistory) {
        List<LeadPortalPaymentHistoryDto> history = new ArrayList<>();

        history.add(new LeadPortalPaymentHistoryDto(
                row.createdAt(),
                "Preferência criada",
                row.status(),
                "system",
                "Checkout registrado"
        ));

        if (!CollectionUtils.isEmpty(webhookHistory)) {
            history.addAll(webhookHistory);
        } else if (StringUtils.hasText(row.mercadoPagoStatus())) {
            history.add(new LeadPortalPaymentHistoryDto(
                    row.updatedAt() != null ? row.updatedAt() : row.createdAt(),
                    "Status no Mercado Pago",
                    row.mercadoPagoStatus(),
                    "system",
                    null
            ));
        }

        if (row.paymentApprovedAt() != null) {
            history.add(new LeadPortalPaymentHistoryDto(
                row.paymentApprovedAt(),
                "Pagamento aprovado",
                "APPROVED",
                "system",
                "Confirmação recebida do Mercado Pago"
            ));
        }

        if (row.deliveredAt() != null) {
            history.add(new LeadPortalPaymentHistoryDto(
                row.deliveredAt(),
                "Entrega concluída",
                "DELIVERED",
                "system",
                null
            ));
        }

        Instant currentStatusAt = row.updatedAt() != null ? row.updatedAt() : row.createdAt();
        history.add(new LeadPortalPaymentHistoryDto(
                currentStatusAt,
                "Status atual",
                row.status(),
                "system",
                row.mercadoPagoStatus()
        ));

        history.sort(Comparator.comparing(LeadPortalPaymentHistoryDto::at, Comparator.nullsLast(Comparator.naturalOrder())));

        JsonNode paymentPayload = parsePaymentPayload(row.mercadoPagoPaymentPayload());

        return new LeadPortalPaymentDto(
                row.id(),
                row.packageId(),
                row.submissionId(),
                row.buyerName(),
                row.buyerEmail(),
                row.status(),
                row.mercadoPagoStatus(),
                row.mercadoPagoPaymentId(),
                row.mercadoPagoPreferenceId(),
                extractPaymentType(paymentPayload),
                extractPaymentMethod(paymentPayload),
                extractRejectionReason(row.status(), row.mercadoPagoStatus(), paymentPayload),
                row.amount(),
                row.currency(),
                row.checkoutExpiresAt(),
                row.paymentApprovedAt(),
                row.deliveredAt(),
                row.createdAt(),
                row.updatedAt(),
                history
        );
    }

    private Map<String, List<LeadPortalPaymentHistoryDto>> fetchWebhookHistory(List<LeadPortalPaymentRow> rows) {
        Set<String> paymentIds = rows.stream()
                .map(LeadPortalPaymentRow::mercadoPagoPaymentId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (paymentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = paymentIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT resource_id, topic, payload_type, payload_action, mercadopago_status, processing_status, created_at "
                + "FROM mercadopago_webhook_log WHERE resource_id IN (" + placeholders + ") ORDER BY created_at";

        List<WebhookHistoryRow> logs = jdbcTemplate.query(sql, ps -> {
            int index = 1;
            for (String id : paymentIds) {
                ps.setString(index++, id);
            }
        }, (rs, rowNum) -> mapWebhookHistory(rs));

        Map<String, List<LeadPortalPaymentHistoryDto>> grouped = new HashMap<>();
        for (WebhookHistoryRow log : logs) {
            if (!StringUtils.hasText(log.resourceId())) {
                continue;
            }
            grouped.computeIfAbsent(log.resourceId(), key -> new ArrayList<>())
                    .add(log.history());
        }

        grouped.replaceAll((id, items) -> items.stream()
                .sorted(Comparator.comparing(LeadPortalPaymentHistoryDto::at))
                .toList());

        return grouped;
    }

    private LeadPortalPaymentRow mapPurchase(ResultSet rs) throws SQLException {
        return new LeadPortalPaymentRow(
                rs.getLong("id"),
                (Long) rs.getObject("package_id"),
                rs.getString("submission_id"),
                rs.getString("buyer_name"),
                rs.getString("buyer_email"),
                rs.getString("status"),
                rs.getString("mp_payment_id"),
                rs.getString("mp_preference_id"),
                rs.getString("mp_status"),
                rs.getString("mp_payment_payload"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                toInstant(rs.getTimestamp("checkout_expires_at")),
                toInstant(rs.getTimestamp("payment_approved_at")),
                toInstant(rs.getTimestamp("delivered_at")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private WebhookHistoryRow mapWebhookHistory(ResultSet rs) throws SQLException {
        Instant createdAt = toInstant(rs.getTimestamp("created_at"));
        String resourceId = rs.getString("resource_id");
        String action = rs.getString("payload_action");
        String status = rs.getString("mercadopago_status");
        String topic = rs.getString("topic");
        String payloadType = rs.getString("payload_type");
        String processingStatus = rs.getString("processing_status");

        StringBuilder label = new StringBuilder("Webhook Mercado Pago");
        if (StringUtils.hasText(payloadType)) {
            label.append(" · ").append(payloadType.toLowerCase());
        }
        if (StringUtils.hasText(action)) {
            label.append(" · ").append(action.toLowerCase());
        }
        if (StringUtils.hasText(topic)) {
            label.append(" · ").append(topic.toLowerCase());
        }

        String finalStatus = StringUtils.hasText(status) ? status : processingStatus;

        LeadPortalPaymentHistoryDto historyDto = new LeadPortalPaymentHistoryDto(
                createdAt,
                label.toString(),
                finalStatus,
                "webhook",
                resourceId
        );

        return new WebhookHistoryRow(resourceId, historyDto);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private record LeadPortalPaymentRow(
            Long id,
            Long packageId,
            String submissionId,
            String buyerName,
            String buyerEmail,
            String status,
            String mercadoPagoPaymentId,
            String mercadoPagoPreferenceId,
            String mercadoPagoStatus,
            String mercadoPagoPaymentPayload,
            BigDecimal amount,
            String currency,
            Instant checkoutExpiresAt,
            Instant paymentApprovedAt,
            Instant deliveredAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    private record WebhookHistoryRow(String resourceId, LeadPortalPaymentHistoryDto history) {
    }

    private JsonNode parsePaymentPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            log.warn("Não foi possível interpretar o payload do pagamento do Mercado Pago", ex);
            return null;
        }
    }

    private String extractPaymentType(JsonNode payload) {
        String value = extractText(payload, "payment_type_id");
        return StringUtils.hasText(value) ? value : null;
    }

    private String extractPaymentMethod(JsonNode payload) {
        String value = extractText(payload, "payment_method_id");
        return StringUtils.hasText(value) ? value : null;
    }

    private String extractRejectionReason(String status, String mercadoPagoStatus, JsonNode payload) {
        boolean rejectedStatus = StringUtils.hasText(status)
                && Set.of("FAILED", "CANCELED", "CANCELLED").contains(status.toUpperCase());
        boolean rejectedMpStatus = StringUtils.hasText(mercadoPagoStatus)
                && "REJECTED".equalsIgnoreCase(mercadoPagoStatus);

        if (!rejectedStatus && !rejectedMpStatus) {
            return null;
        }

        return extractText(payload, "status_detail");
    }

    private String extractText(JsonNode payload, String field) {
        if (payload == null) {
            return null;
        }
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value : null;
    }
}
