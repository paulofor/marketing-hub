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
                        6700))),
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
        PepperTransactionSyncService syncService = new PepperTransactionSyncService(
                accessService,
                new StaticPepperGateway(List.of(new PepperPaidTransaction(
                        "tx-paid-67",
                        "cliente@sandbox.local",
                        "paid",
                        "owm6x",
                        "Metodo MUSA",
                        6700))),
                "metodo-musa-7-dias");

        var first = syncService.syncPaidTransactions("metodo-musa-7-dias", "cliente@sandbox.local");
        var second = syncService.syncPaidTransactions("metodo-musa-7-dias", "cliente@sandbox.local");

        assertThat(second.accesses().getFirst().token()).isEqualTo(first.accesses().getFirst().token());
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
