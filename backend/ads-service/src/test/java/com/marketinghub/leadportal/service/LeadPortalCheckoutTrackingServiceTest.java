package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.leadportal.config.LeadPortalPaymentLinkProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LeadPortalCheckoutTrackingServiceTest {

    private JdbcTemplate jdbcTemplate;
    private LeadPortalCheckoutTrackingService trackingService;

    @BeforeEach
    void setUp() {
        String dbName = "checkout-tracking-" + UUID.randomUUID();
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:%s;MODE=MySQL;DB_CLOSE_DELAY=-1".formatted(dbName), "sa", "");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                "CREATE TABLE lead_portal_purchase ("
                        + "id BIGINT PRIMARY KEY,"
                        + "package_id BIGINT,"
                        + "submission_id VARCHAR(64),"
                        + "checkout_url VARCHAR(255),"
                        + "checkout_expires_at TIMESTAMP NULL,"
                        + "checkout_accessed_at TIMESTAMP NULL,"
                        + "updated_at TIMESTAMP NULL"
                        + ")");

        LeadPortalPaymentLinkProperties properties = new LeadPortalPaymentLinkProperties();
        properties.setEntrypointBaseUrl("https://pagamentopalf.site/checkout");
        trackingService = new LeadPortalCheckoutTrackingService(jdbcTemplate, properties);
    }

    @Test
    void registerCheckoutAccessMarksTimestampAndReturnsRedirect() {
        String submissionId = "f3c8979c-47a6-4f60-9355-f41a2d6fc9e5";
        jdbcTemplate.update(
                "INSERT INTO lead_portal_purchase (id, package_id, submission_id, checkout_url, checkout_expires_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                10L,
                77L,
                submissionId,
                "https://www.mercadopago.com.br/checkout?pref_id=123",
                Instant.now());

        Optional<LeadPortalCheckoutTrackingService.CheckoutRedirect> redirect =
                trackingService.registerCheckoutAccess(10L, submissionId);

        assertThat(redirect).isPresent();
        assertThat(redirect.get().url())
                .contains("https://pagamentopalf.site/checkout")
                .contains("packageId=77")
                .contains("purchaseId=10");
        Instant accessedAt = jdbcTemplate.queryForObject(
                "SELECT checkout_accessed_at FROM lead_portal_purchase WHERE id = ?",
                (rs, rowNum) -> rs.getTimestamp(1).toInstant(),
                10L);
        assertThat(accessedAt).isNotNull();
    }
}
