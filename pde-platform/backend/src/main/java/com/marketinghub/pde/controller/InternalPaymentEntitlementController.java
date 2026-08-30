package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.InternalPaymentEntitlementRequest;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.marketinghub.pde.service.RigelPaidEntitlementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe um provedor financeiro fictício somente para a homologação interna segregada. */
@RestController
@RequestMapping("/api/internal/pde/test-payment-entitlements")
@ConditionalOnProperty(name = "pde.access.internal-qa-enabled", havingValue = "true")
public class InternalPaymentEntitlementController {
    private final RigelPaidEntitlementService entitlementService;
    private final AccessService accessService;
    private final InternalApiAuthorizer internalApiAuthorizer;
    private final boolean internalQaEnabled;

    /** Recebe a guarda financeira, a autorização interna e a trava explícita do ambiente. */
    public InternalPaymentEntitlementController(
            RigelPaidEntitlementService entitlementService,
            AccessService accessService,
            InternalApiAuthorizer internalApiAuthorizer,
            @Value("${pde.access.internal-qa-enabled:false}") boolean internalQaEnabled) {
        this.entitlementService = entitlementService;
        this.accessService = accessService;
        this.internalApiAuthorizer = internalApiAuthorizer;
        this.internalQaEnabled = internalQaEnabled;
    }

    /** Registra aprovação ou reembolso fictício sem criar venda ou receita humana. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RigelPaidEntitlementService.InternalPaymentResult record(
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody InternalPaymentEntitlementRequest request) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        if (!internalQaEnabled) {
            throw new SecurityException("Pagamento de homologação PDE está desabilitado neste ambiente");
        }
        RigelPaidEntitlementService.InternalPaymentResult result = entitlementService.recordInternalQaPayment(
                request.email(),
                request.transactionId(),
                request.paymentStatus(),
                request.experienceVersion());
        if ("refunded".equalsIgnoreCase(result.paymentStatus())
                || "charged_back".equalsIgnoreCase(result.paymentStatus())) {
            accessService.revokeMercadoPagoPaidAccess(
                    request.email(), result.transactionId(), result.paymentStatus());
        }
        return result;
    }
}
