package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Valida a reconciliação completa de reembolso entre provedor, finanças, acesso e retry. */
class PepperRefundSyncServiceTest {
    @TempDir Path tempDir;

    /** Revoga a compra uma vez e mantém o retry idempotente sem recontar o reembolso. */
    @Test
    void reconcilesRefundAndRevokesPaidAccessIdempotently() {
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("refund-access.json").toString());
        PaymentAuditService auditService = new PaymentAuditService("", "", "");
        PepperPaidTransaction payment = new PepperPaidTransaction(
                "tx-refund-67", "cliente@sandbox.local", "paid", "owm6x", "MUSA", 6700, "BRL", "musa-pde-entry-v7-espelho-antes-de-sair");
        auditService.recordVerifiedPayment("metodo-musa-7-dias", payment);
        var access = accessService.releasePepperPaidTransaction(
                "metodo-musa-7-dias",
                payment.buyerEmail(),
                payment.transactionId(),
                payment.offerHash(),
                payment.amount(),
                payment.currency(),
                payment.paymentStatus(),
                payment.experienceVersion(),
                true);
        auditService.linkReleasedAccess(payment.transactionId(), access.token());
        PepperTransactionGateway gateway = mock(PepperTransactionGateway.class);
        when(gateway.findTransactionByHash("tx-refund-67"))
                .thenReturn(new PepperTransactionSnapshot(
                        "tx-refund-67",
                        "cliente@sandbox.local",
                        "refunded",
                        "owm6x",
                        "MUSA",
                        6700,
                        "BRL",
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        PepperRefundSyncService service =
                new PepperRefundSyncService(gateway, auditService, accessService, "metodo-musa-7-dias");

        var first = service.reconcile("metodo-musa-7-dias", "tx-refund-67");
        var retry = service.reconcile("metodo-musa-7-dias", "tx-refund-67");

        assertThat(first.newlyRecorded()).isTrue();
        assertThat(first.accessRevoked()).isTrue();
        assertThat(retry.newlyRecorded()).isFalse();
        assertThat(retry.accessRevoked()).isFalse();
        assertThat(accessService.getWorkspace(access.token()).subscriptionStatus()).isEqualTo("REFUNDED");
        assertThatThrownBy(() -> accessService.authorizeMaterialAccess(access.token()))
                .isInstanceOf(SecurityException.class);
    }
}
