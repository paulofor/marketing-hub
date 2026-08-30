package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.MercadoPagoEntitlementRequest;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.MercadoPagoEntitlementAuthorizer;
import com.marketinghub.pde.service.RigelPaidEntitlementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida a fronteira HTTP autenticada usada pelo webhook financeiro do Kit. */
class MercadoPagoEntitlementControllerTest {

    /** Aceita o segredo exato e encaminha o pagamento sem alterar seu contrato. */
    @Test
    void reconcilesAuthenticatedPayment() {
        RigelPaidEntitlementService service = mock(RigelPaidEntitlementService.class);
        AccessService accessService = mock(AccessService.class);
        MercadoPagoEntitlementRequest request = request();
        var expected = new RigelPaidEntitlementService.PaymentReconciliationResult(
                "mp-271", "approved", "RECORDED", "2026-08-30T04:00:00Z");
        when(service.recordVerifiedPayment(request)).thenReturn(expected);
        MercadoPagoEntitlementController controller = new MercadoPagoEntitlementController(
                new MercadoPagoEntitlementAuthorizer("payment-test-secret"), service, accessService);

        var result = controller.reconcile("Bearer payment-test-secret", request);

        assertThat(result).isEqualTo(expected);
        verify(service).recordVerifiedPayment(request);
        verifyNoInteractions(accessService);
    }

    /** Revoga o grant correlacionado somente depois de persistir o reembolso confirmado. */
    @Test
    void revokesAccessAfterRefundReconciliation() {
        RigelPaidEntitlementService service = mock(RigelPaidEntitlementService.class);
        AccessService accessService = mock(AccessService.class);
        MercadoPagoEntitlementRequest request = new MercadoPagoEntitlementRequest(
                "mp-271",
                "refunded",
                new BigDecimal("349.00"),
                "BRL",
                "buyer@sandbox.local",
                RigelPaidEntitlementService.PRODUCT_SLUG,
                Instant.parse("2026-08-30T04:00:00Z"),
                Map.of(
                        "productKey", RigelPaidEntitlementService.PRODUCT_SLUG,
                        "productId", 9,
                        "experimentId", 89));
        var expected = new RigelPaidEntitlementService.PaymentReconciliationResult(
                "mp-271", "refunded", "DUPLICATE_OR_UPDATED", "2026-08-30T04:00:00Z");
        when(service.recordVerifiedPayment(request)).thenReturn(expected);
        MercadoPagoEntitlementController controller = new MercadoPagoEntitlementController(
                new MercadoPagoEntitlementAuthorizer("payment-test-secret"), service, accessService);

        controller.reconcile("Bearer payment-test-secret", request);

        verify(accessService).revokeMercadoPagoPaidAccess(
                "buyer@sandbox.local", "mp-271", "refunded");
    }

    /** Bloqueia cabeçalho ausente antes de permitir qualquer escrita financeira. */
    @Test
    void rejectsMissingCredential() {
        RigelPaidEntitlementService service = mock(RigelPaidEntitlementService.class);
        MercadoPagoEntitlementController controller = new MercadoPagoEntitlementController(
                new MercadoPagoEntitlementAuthorizer("payment-test-secret"), service, mock(AccessService.class));

        assertThatThrownBy(() -> controller.reconcile(null, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    /** Cria um payload válido e sem dados alheios à conciliação. */
    private MercadoPagoEntitlementRequest request() {
        return new MercadoPagoEntitlementRequest(
                "mp-271",
                "approved",
                new BigDecimal("349.00"),
                "BRL",
                "buyer@sandbox.local",
                RigelPaidEntitlementService.PRODUCT_SLUG,
                Instant.parse("2026-08-30T04:00:00Z"),
                Map.of(
                        "productKey", RigelPaidEntitlementService.PRODUCT_SLUG,
                        "productId", 9,
                        "experimentId", 89));
    }
}
