package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.AccessResponse;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Valida a reconciliacao de compras Pepper quando o webhook nao chega ao PDE. */
class PepperTransactionSyncServiceTest {

    @TempDir
    Path tempDir;

    /** Confirma que transacao paga da oferta MUSA libera acesso ativo. */
    @Test
    void releasesAccessFromPaidPepperTransactionSync() {
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());
        PepperTransactionSyncService syncService = new PepperTransactionSyncService(
                accessService,
                new StaticPepperGateway(List.of(new PepperPaidTransaction(
                        "tx-paid-67",
                        "Cliente@Sandbox.Local",
                        "paid",
                        "owm6x",
                        "Metodo MUSA",
                        6700,
                        "BRL"))),
                new PaymentAuditService("", "", ""),
                "metodo-musa-7-dias");

        var response = syncService.syncPaidTransactions(null, "cliente@sandbox.local");
        AccessResponse access = response.accesses().getFirst();
        var workspace = accessService.getWorkspace(access.token());

        assertThat(response.scannedTransactions()).isEqualTo(1);
        assertThat(response.paidTransactions()).isEqualTo(1);
        assertThat(response.releasedAccesses()).isEqualTo(1);
        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("PEPPER");
    }

    /** Confirma que repetir a reconciliacao nao cria outro acesso para a mesma compradora. */
    @Test
    void keepsPepperSyncIdempotentForSameBuyerEmail() {
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());
        PaymentAuditService paymentAuditService = new PaymentAuditService("", "", "");
        PepperTransactionSyncService syncService = new PepperTransactionSyncService(
                accessService,
                new StaticPepperGateway(List.of(new PepperPaidTransaction(
                        "tx-paid-67",
                        "cliente@sandbox.local",
                        "paid",
                        "owm6x",
                        "Metodo MUSA",
                        6700,
                        "BRL"))),
                paymentAuditService,
                "metodo-musa-7-dias");

        var first = syncService.syncPaidTransactions("metodo-musa-7-dias", "cliente@sandbox.local");
        var second = syncService.syncPaidTransactions("metodo-musa-7-dias", "cliente@sandbox.local");

        assertThat(second.accesses().getFirst().token()).isEqualTo(first.accesses().getFirst().token());
        assertThat(paymentAuditService.findForTesting("tx-paid-67")).hasValueSatisfying(audit -> {
            assertThat(audit.productSlug()).isEqualTo("metodo-musa-7-dias");
            assertThat(audit.amountCents()).isEqualTo(6700);
            assertThat(audit.currency()).isEqualTo("BRL");
            assertThat(audit.accessReferenceHash()).hasSize(64);
        });
    }

    /** Impede que a mesma transação libere acesso para outro produto ou outra compradora. */
    @Test
    void blocksReuseOfTransactionWithDifferentFinancialContract() {
        PaymentAuditService paymentAuditService = new PaymentAuditService("", "", "");
        PepperPaidTransaction original = new PepperPaidTransaction(
                "tx-reused", "cliente@sandbox.local", "paid", "owm6x", "MUSA", 6700, "BRL");
        PepperPaidTransaction collision = new PepperPaidTransaction(
                "tx-reused", "outra@sandbox.local", "paid", "owm6x", "MUSA", 6700, "BRL");

        paymentAuditService.recordVerifiedPayment("metodo-musa-7-dias", original);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> paymentAuditService.recordVerifiedPayment("outro-produto", collision));
    }

    /** Retorna uma lista fixa de transacoes pagas para simular a API Pepper. */
    private record StaticPepperGateway(List<PepperPaidTransaction> transactions) implements PepperTransactionGateway {

        /** Entrega transacoes pagas ja filtradas para o servico de sincronizacao. */
        @Override
        public PepperTransactionSearchResult findPaidTransactions(String search) {
            return new PepperTransactionSearchResult(transactions.size(), transactions);
        }

        /** Entrega transacoes pagas por hash para simular recuperacao de compra especifica. */
        @Override
        public PepperTransactionSearchResult findPaidTransactionByHash(String transactionHash) {
            return new PepperTransactionSearchResult(transactions.size(), transactions);
        }
    }
}
