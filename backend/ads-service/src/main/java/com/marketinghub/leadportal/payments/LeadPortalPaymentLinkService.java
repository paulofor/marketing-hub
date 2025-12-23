package com.marketinghub.leadportal.payments;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadPortalPaymentLinkService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPaymentLinkService.class);

    private final JdbcTemplate jdbcTemplate;
    private final LeadPortalPaymentsClient paymentsClient;

    public LeadPortalPaymentLinkService(JdbcTemplate jdbcTemplate,
                                        LeadPortalPaymentsClient paymentsClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentsClient = paymentsClient;
    }

    public Optional<String> resolveCheckoutLink(long packageId, String buyerEmail, String buyerName) {
        if (!paymentsClient.isEnabled()) {
            return Optional.empty();
        }
        Optional<String> existingLink = findExistingCheckoutLink(packageId);
        if (existingLink.isPresent()) {
            return existingLink;
        }
        return paymentsClient.createCheckout(packageId, buyerEmail, buyerName)
                .map(LeadPortalCheckoutResponse::checkoutUrl)
                .filter(StringUtils::hasText);
    }

    private Optional<String> findExistingCheckoutLink(long packageId) {
        String sql = """
                SELECT checkout_url
                FROM lead_portal_purchase
                WHERE package_id = ?
                  AND checkout_url IS NOT NULL
                  AND status IN ('PREFERENCE_CREATED', 'PENDING_PAYMENT')
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try {
            return jdbcTemplate.query(sql, ps -> ps.setLong(1, packageId), rs -> {
                if (rs.next()) {
                    String url = rs.getString("checkout_url");
                    if (StringUtils.hasText(url)) {
                        return Optional.of(url);
                    }
                }
                return Optional.empty();
            });
        } catch (DataAccessException ex) {
            log.debug("Não foi possível consultar lead_portal_purchase para o pacote {}", packageId, ex);
            return Optional.empty();
        }
    }
}
