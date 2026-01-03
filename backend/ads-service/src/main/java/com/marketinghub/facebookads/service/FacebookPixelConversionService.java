package com.marketinghub.facebookads.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class FacebookPixelConversionService {

    private final JdbcTemplate jdbcTemplate;

    public FacebookPixelConversionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PixelConversion> listApprovedPurchasesPendingPixel(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT
                    p.id AS purchase_id,
                    p.mp_payment_id,
                    p.amount,
                    p.currency,
                    p.payment_approved_at,
                    p.created_at,
                    exp.id AS experiment_id,
                    exp.name AS experiment_name,
                    exp.facebook_pixel_id
                FROM lead_portal_purchase p
                JOIN flow_submission_image_package pack ON pack.payment_purchase_id = p.id
                JOIN flow_submissions sub ON sub.id = pack.submission_id
                JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                WHERE p.status = 'APPROVED'
                  AND p.pixel_conversion_recorded_at IS NULL
                  AND exp.facebook_pixel_id IS NOT NULL
                ORDER BY p.created_at ASC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, ps -> ps.setInt(1, safeLimit), (rs, rowNum) -> mapConversion(rs));
    }

    public void markConversionRecorded(long purchaseId) {
        jdbcTemplate.update(
                "UPDATE lead_portal_purchase SET pixel_conversion_recorded_at = UTC_TIMESTAMP() WHERE id = ?",
                purchaseId
        );
    }

    private PixelConversion mapConversion(ResultSet rs) throws SQLException {
        return new PixelConversion(
                rs.getLong("purchase_id"),
                (Long) rs.getObject("experiment_id"),
                rs.getString("experiment_name"),
                rs.getString("facebook_pixel_id"),
                rs.getString("mp_payment_id"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                toInstant(rs.getTimestamp("payment_approved_at"), rs.getTimestamp("created_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp, Timestamp fallback) {
        if (timestamp != null) {
            return timestamp.toInstant();
        }
        return fallback != null ? fallback.toInstant() : null;
    }

    public record PixelConversion(
            Long purchaseId,
            Long experimentId,
            String experimentName,
            String pixelId,
            String paymentId,
            BigDecimal amount,
            String currency,
            Instant paymentApprovedAt
    ) {
        public String normalizedCurrency() {
            return StringUtils.hasText(currency) ? currency.trim().toUpperCase() : null;
        }
    }
}
