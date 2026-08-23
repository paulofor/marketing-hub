package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.PepperSyncResponse;
import com.marketinghub.pde.dto.PepperWebhookRequest;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.marketinghub.pde.service.PepperTransactionSyncService;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida que o webhook público somente libera acesso após reconciliação na Pepper. */
class AccessControllerPepperWebhookTest {

    /** Retorna acesso somente quando a API Pepper comprova uma única compra compatível. */
    @Test
    void releasesAccessAfterProviderReconciliation() {
        PepperTransactionSyncService syncService = mock(PepperTransactionSyncService.class);
        AccessResponse expected = new AccessResponse(
                "token-pago", "metodo-musa-7-dias", "cliente@sandbox.local", "PEPPER", "/access/token-pago");
        when(syncService.syncPaidTransactions("metodo-musa-7-dias", null, "tx-67"))
                .thenReturn(new PepperSyncResponse("metodo-musa-7-dias", 1, 1, 1, List.of(expected)));
        AccessController controller = new AccessController(
                mock(AccessService.class), syncService, new InternalApiAuthorizer("segredo-interno"));

        AccessResponse actual = controller.receivePepperWebhook(paidWebhook("tx-67"));

        assertThat(actual).isEqualTo(expected);
        verify(syncService).syncPaidTransactions("metodo-musa-7-dias", null, "tx-67");
    }

    /** Recusa payload pago que não seja comprovado pela consulta ao provedor. */
    @Test
    void rejectsUnverifiedPaidPayload() {
        PepperTransactionSyncService syncService = mock(PepperTransactionSyncService.class);
        when(syncService.syncPaidTransactions("metodo-musa-7-dias", null, "tx-falsa"))
                .thenReturn(new PepperSyncResponse("metodo-musa-7-dias", 0, 0, 0, List.of()));
        AccessController controller = new AccessController(
                mock(AccessService.class), syncService, new InternalApiAuthorizer("segredo-interno"));

        assertThatThrownBy(() -> controller.receivePepperWebhook(paidWebhook("tx-falsa")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não comprovada");
    }

    /** Monta um aviso de pagamento que ainda precisa ser confirmado no provedor. */
    private PepperWebhookRequest paidWebhook(String transactionId) {
        return new PepperWebhookRequest(
                "metodo-musa-7-dias",
                "cliente@sandbox.local",
                transactionId,
                "paid",
                null,
                null,
                null,
                null,
                null);
    }
}
