package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida durabilidade, idempotência e minimização da trilha financeira do PDE. */
class PaymentAuditServiceTest {

    /** Persiste o pagamento sem PII e recupera a mesma trilha após recriar o serviço. */
    @Test
    void persistsAndReloadsHashedFinancialAudit() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:payment-audit-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
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
        PepperPaidTransaction transaction = new PepperPaidTransaction(
                "tx-durable", "cliente@sandbox.local", "paid", "owm6x", "MUSA", 6700, "BRL", "musa-v7");
        PaymentAuditService service = new PaymentAuditService(jdbcUrl, "sa", "");

        service.recordVerifiedPayment("metodo-musa-7-dias", transaction);
        service.linkReleasedAccess("tx-durable", "bearer-secret-token");
        PaymentAuditService restarted = new PaymentAuditService(jdbcUrl, "sa", "");
        var audit = restarted.findForTesting("tx-durable").orElseThrow();

        assertThat(audit.amountCents()).isEqualTo(6700);
        assertThat(audit.currency()).isEqualTo("BRL");
        assertThat(audit.experienceVersion()).isEqualTo("musa-v7");
        assertThat(audit.buyerReferenceHash()).hasSize(64).doesNotContain("cliente");
        assertThat(audit.accessReferenceHash()).hasSize(64).doesNotContain("bearer");
        assertThat(audit.accessReleasedAt()).isNotNull();
        assertThrows(IllegalArgumentException.class, () -> restarted.recordVerifiedPayment(
                "outro-produto",
                new PepperPaidTransaction(
                        "tx-durable", "cliente@sandbox.local", "paid", "owm6x", "MUSA", 6700, "BRL", "musa-v7")));
    }

    /** Registra o reembolso uma vez e preserva a referência do acesso para revogação. */
    @Test
    void recordsVerifiedRefundIdempotently() {
        PaymentAuditService service = new PaymentAuditService("", "", "");
        PepperPaidTransaction payment = new PepperPaidTransaction(
                "tx-refund", "cliente@sandbox.local", "paid", "owm6x", "MUSA", 6700, "BRL", "musa-v7");
        service.recordVerifiedPayment("metodo-musa-7-dias", payment);
        service.linkReleasedAccess("tx-refund", "token-refund");
        PepperTransactionSnapshot refund = new PepperTransactionSnapshot(
                "tx-refund", "cliente@sandbox.local", "refunded", "owm6x", "MUSA", 6700, "BRL", "musa-v7");

        var first = service.recordVerifiedRefund("metodo-musa-7-dias", refund);
        var retry = service.recordVerifiedRefund("metodo-musa-7-dias", refund);

        assertThat(first.newlyRecorded()).isTrue();
        assertThat(first.accessReferenceHash()).hasSize(64);
        assertThat(retry.newlyRecorded()).isFalse();
        assertThat(service.findForTesting("tx-refund").orElseThrow().paymentStatus())
                .isEqualTo("refunded");
    }
}
