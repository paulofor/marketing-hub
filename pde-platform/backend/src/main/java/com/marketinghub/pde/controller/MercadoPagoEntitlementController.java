package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.MercadoPagoEntitlementRequest;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.MercadoPagoEntitlementAuthorizer;
import com.marketinghub.pde.service.RigelPaidEntitlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recebe do serviço oficial a aprovação ou revogação financeira do Kit WhatsApp Pronto. */
@RestController
@RequestMapping("/api/internal/pde/mercado-pago/entitlements")
public class MercadoPagoEntitlementController {
    private final MercadoPagoEntitlementAuthorizer authorizer;
    private final RigelPaidEntitlementService entitlementService;
    private final AccessService accessService;

    /** Configura a autenticação interna e a guarda financeira única do produto Rigel. */
    public MercadoPagoEntitlementController(
            MercadoPagoEntitlementAuthorizer authorizer,
            RigelPaidEntitlementService entitlementService,
            AccessService accessService) {
        this.authorizer = authorizer;
        this.entitlementService = entitlementService;
        this.accessService = accessService;
    }

    /** Persiste o estado confirmado e devolve somente referências sanitizadas. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RigelPaidEntitlementService.PaymentReconciliationResult reconcile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody MercadoPagoEntitlementRequest request) {
        authorizer.requireAuthorized(authorization);
        RigelPaidEntitlementService.PaymentReconciliationResult result =
                entitlementService.recordVerifiedPayment(request);
        if ("refunded".equalsIgnoreCase(result.paymentStatus())
                || "charged_back".equalsIgnoreCase(result.paymentStatus())) {
            accessService.revokeMercadoPagoPaidAccess(
                    request.buyerEmail(), result.transactionId(), result.paymentStatus());
        }
        return result;
    }
}
