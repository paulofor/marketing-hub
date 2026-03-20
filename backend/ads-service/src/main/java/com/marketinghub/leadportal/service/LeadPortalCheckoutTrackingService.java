package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.config.LeadPortalPaymentLinkProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Registra acessos públicos ao link do checkout e encaminha o lead para o destino correto.
 */
@Service
public class LeadPortalCheckoutTrackingService {

    private final JdbcTemplate jdbcTemplate;
    private final LeadPortalPaymentLinkProperties paymentLinkProperties;

    public LeadPortalCheckoutTrackingService(JdbcTemplate jdbcTemplate,
                                             LeadPortalPaymentLinkProperties paymentLinkProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentLinkProperties = paymentLinkProperties;
    }

    public Optional<CheckoutRedirect> registerCheckoutAccess(long purchaseId, String submissionToken) {
        Optional<CheckoutTarget> target = loadTarget(purchaseId);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        if (!matchesSubmission(target.get(), submissionToken)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(target.get().checkoutUrl())) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        jdbcTemplate.update(
                "UPDATE lead_portal_purchase SET checkout_accessed_at = COALESCE(checkout_accessed_at, ?), updated_at = ? "
                        + "WHERE id = ?",
                timestamp,
                timestamp,
                purchaseId);
        String redirectUrl = resolveRedirectUrl(target.get());
        return Optional.of(new CheckoutRedirect(redirectUrl));
    }

    private Optional<CheckoutTarget> loadTarget(long purchaseId) {
        return jdbcTemplate.query(
                        "SELECT id, package_id, submission_id, checkout_url, checkout_expires_at "
                                + "FROM lead_portal_purchase WHERE id = ?",
                        (rs, rowNum) -> mapTarget(rs),
                        purchaseId)
                .stream()
                .findFirst();
    }

    private CheckoutTarget mapTarget(ResultSet rs) throws SQLException {
        long purchaseId = rs.getLong("id");
        Long packageId = (Long) rs.getObject("package_id");
        String submissionId = readSubmissionId(rs.getObject("submission_id"));
        String checkoutUrl = rs.getString("checkout_url");
        Instant expiresAt = toInstant(rs.getTimestamp("checkout_expires_at"));
        return new CheckoutTarget(purchaseId, packageId, submissionId, checkoutUrl, expiresAt);
    }

    private boolean matchesSubmission(CheckoutTarget target, String submissionToken) {
        if (!StringUtils.hasText(target.submissionId())) {
            return true;
        }
        if (!StringUtils.hasText(submissionToken)) {
            return false;
        }
        return target.submissionId().equalsIgnoreCase(submissionToken.trim());
    }

    private String resolveRedirectUrl(CheckoutTarget target) {
        if (!StringUtils.hasText(paymentLinkProperties.getEntrypointBaseUrl())) {
            return target.checkoutUrl();
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                paymentLinkProperties.getEntrypointBaseUrl().trim());
        if (StringUtils.hasText(paymentLinkProperties.getPackageIdQueryParam()) && target.packageId() != null) {
            builder.replaceQueryParam(paymentLinkProperties.getPackageIdQueryParam(), target.packageId());
        }
        if (StringUtils.hasText(paymentLinkProperties.getPurchaseIdQueryParam())) {
            builder.replaceQueryParam(paymentLinkProperties.getPurchaseIdQueryParam(), target.purchaseId());
        }
        if (target.expiresAt() != null) {
            builder.replaceQueryParam("expiresAt", target.expiresAt());
        }
        return builder.build(true).toUriString();
    }

    private String readSubmissionId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof byte[] bytes) {
            try {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
                UUID submissionId = new UUID(buffer.getLong(), buffer.getLong());
                return submissionId.toString();
            } catch (Exception ex) {
                return null;
            }
        }
        String value = raw.toString();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record CheckoutRedirect(String url) {
    }

    private record CheckoutTarget(
            long purchaseId,
            Long packageId,
            String submissionId,
            String checkoutUrl,
            Instant expiresAt) {
    }
}
