package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pde.dto.MercadoPagoEntitlementRequest;
import com.marketinghub.pde.model.AccessGrant;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida a fonte única de compra, vínculo e revogação do acesso pago do produto Rigel. */
class RigelPaidEntitlementServiceTest {
    private RigelPaidEntitlementService service;

    /** Cria uma trilha MySQL compatível e nova para cada cenário financeiro. */
    @BeforeEach
    void setUp() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:rigel-entitlement-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE pde_payment_audit (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      provider VARCHAR(40) NOT NULL,
                      transaction_id VARCHAR(191) NOT NULL,
                      product_slug VARCHAR(191) NOT NULL,
                      experience_version VARCHAR(80),
                      offer_hash VARCHAR(191) NOT NULL,
                      amount_cents INT NOT NULL,
                      currency VARCHAR(3) NOT NULL,
                      payment_status VARCHAR(40) NOT NULL,
                      buyer_reference_hash CHAR(64) NOT NULL,
                      access_reference_hash CHAR(64),
                      verified_at DATETIME NOT NULL,
                      access_released_at DATETIME,
                      refunded_at DATETIME,
                      UNIQUE (provider, transaction_id)
                    )
                    """);
        }
        service = new RigelPaidEntitlementService(jdbcUrl, "sa", "");
    }

    /** Libera uma vez a versão exata após aprovação e preserva a identidade no retry. */
    @Test
    void recordsApprovedPaymentAndClaimsOnlyOneToken() {
        var first = service.recordVerifiedPayment(payment("approved", "buyer@sandbox.local"));
        var retry = service.recordVerifiedPayment(payment("approved", "buyer@sandbox.local"));
        service.claimApprovedPayment("buyer@sandbox.local", "access-token-271");
        AccessGrant grant = grant("buyer@sandbox.local", "access-token-271");

        service.requireActiveAccess(grant);

        assertThat(first.result()).isEqualTo("RECORDED");
        assertThat(retry.result()).isEqualTo("DUPLICATE_OR_UPDATED");
        assertThatThrownBy(() -> service.claimApprovedPayment(
                        "buyer@sandbox.local", "different-access-token"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("outro acesso");
    }

    /** Revoga imediatamente o mesmo token e mantém o reembolso idempotente. */
    @Test
    void blocksPaidBoundaryAfterRefund() {
        service.recordVerifiedPayment(payment("approved", "refunded@sandbox.local"));
        service.claimApprovedPayment("refunded@sandbox.local", "refunded-token");
        AccessGrant grant = grant("refunded@sandbox.local", "refunded-token");
        var refund = service.recordVerifiedPayment(payment("refunded", "refunded@sandbox.local"));
        var refundRetry = service.recordVerifiedPayment(payment("refunded", "refunded@sandbox.local"));

        assertThat(refund.result()).isEqualTo("DUPLICATE_OR_UPDATED");
        assertThat(refundRetry.result()).isEqualTo("DUPLICATE_OR_UPDATED");
        assertThatThrownBy(() -> service.requireActiveAccess(grant))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("reembolsado");
    }

    /** Rejeita preço, atribuição ou compradora divergentes da transação original. */
    @Test
    void rejectsCommercialMismatchAndTransactionReuse() {
        service.recordVerifiedPayment(payment("approved", "original@sandbox.local"));
        MercadoPagoEntitlementRequest wrongAmount = new MercadoPagoEntitlementRequest(
                "mp-271-wrong",
                "approved",
                new BigDecimal("348.99"),
                "BRL",
                "original@sandbox.local",
                RigelPaidEntitlementService.PRODUCT_SLUG,
                Instant.parse("2026-08-30T04:00:00Z"),
                metadata());

        assertThatThrownBy(() -> service.recordVerifiedPayment(wrongAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oferta aprovada");
        assertThatThrownBy(() -> service.recordVerifiedPayment(payment(
                        "approved", "other-buyer@sandbox.local")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contrato financeiro diferente");
    }

    /** Mantém uma compra posterior ativa quando chega reembolso atrasado da transação anterior. */
    @Test
    void doesNotRevokeNewerApprovedPurchaseWhenOlderPaymentIsRefunded() {
        String email = "repeat-buyer@sandbox.local";
        String token = "repeat-buyer-token";
        service.recordVerifiedPayment(payment(
                "mp-old", "approved", email, Instant.parse("2026-08-30T03:00:00Z")));
        service.claimApprovedPayment(email, token);
        service.recordVerifiedPayment(payment(
                "mp-new", "approved", email, Instant.parse("2026-08-30T04:00:00Z")));
        service.claimApprovedPayment(email, token);

        service.recordVerifiedPayment(payment(
                "mp-old", "refunded", email, Instant.parse("2026-08-30T03:00:00Z")));

        assertThat(service.shouldRevokeAccess(email, "mp-old", token)).isFalse();
        service.requireActiveAccess(grant(email, token));
    }

    /** Impede que webhook aprovado atrasado reverta uma decisão terminal de reembolso. */
    @Test
    void rejectsApprovedReplayAfterRefund() {
        service.recordVerifiedPayment(payment("approved", "terminal@sandbox.local"));
        service.recordVerifiedPayment(payment("refunded", "terminal@sandbox.local"));

        assertThatThrownBy(() -> service.recordVerifiedPayment(payment(
                        "approved", "terminal@sandbox.local")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode voltar");
    }

    /** Monta o payload fiel ao checkout comercial do experimento 89. */
    private MercadoPagoEntitlementRequest payment(String status, String email) {
        return payment(
                "mp-271", status, email, Instant.parse("2026-08-30T04:00:00Z"));
    }

    /** Permite variar identidade e data para reproduzir recompras e webhooks fora de ordem. */
    private MercadoPagoEntitlementRequest payment(
            String transactionId, String status, String email, Instant approvedAt) {
        return new MercadoPagoEntitlementRequest(
                transactionId,
                status,
                new BigDecimal("349.00"),
                "BRL",
                email,
                RigelPaidEntitlementService.PRODUCT_SLUG,
                approvedAt,
                metadata());
    }

    /** Preserva os identificadores que atribuem a compra ao produto e ao experimento corretos. */
    private Map<String, Object> metadata() {
        return Map.of(
                "productKey", RigelPaidEntitlementService.PRODUCT_SLUG,
                "productId", 9,
                "experimentId", 89);
    }

    /** Cria o grant que representa a área paga já entregue à compradora. */
    private AccessGrant grant(String email, String token) {
        return new AccessGrant(
                token,
                RigelPaidEntitlementService.PRODUCT_SLUG,
                email,
                RigelPaidEntitlementService.PAID_SOURCE,
                Instant.parse("2026-08-30T04:00:01Z"),
                RigelPaidEntitlementService.EXPERIENCE_VERSION,
                Instant.parse("2026-08-30T04:00:00Z"),
                null);
    }
}
