package com.marketinghub.payments.controller;

import com.marketinghub.payments.dto.CreateCheckoutRequest;
import com.marketinghub.payments.dto.CreateCheckoutResponse;
import com.marketinghub.payments.dto.TemporaryCheckoutRequest;
import com.marketinghub.payments.dto.TemporaryCheckoutResponse;
import com.marketinghub.payments.service.CheckoutService;
import com.marketinghub.payments.service.PremiumDeliveryService;
import com.marketinghub.payments.service.TemporaryCheckoutService;
import com.marketinghub.payments.service.TemporaryCheckoutAdminAuthorizer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;

/** Expõe operações de checkout, entrega e testes temporários de pagamento. */
@RestController
@RequestMapping("/api/v1/payments")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final PremiumDeliveryService premiumDeliveryService;
    private final TemporaryCheckoutService temporaryCheckoutService;
    private final TemporaryCheckoutAdminAuthorizer temporaryCheckoutAdminAuthorizer;

    /** Configura os serviços usados pelas operações de pagamento. */
    public CheckoutController(CheckoutService checkoutService, PremiumDeliveryService premiumDeliveryService,
                              TemporaryCheckoutService temporaryCheckoutService,
                              TemporaryCheckoutAdminAuthorizer temporaryCheckoutAdminAuthorizer) {
        this.checkoutService = checkoutService;
        this.premiumDeliveryService = premiumDeliveryService;
        this.temporaryCheckoutService = temporaryCheckoutService;
        this.temporaryCheckoutAdminAuthorizer = temporaryCheckoutAdminAuthorizer;
    }

    /** Cria ou reutiliza o checkout de um pacote pronto. */
    @PostMapping("/checkout")
    public CreateCheckoutResponse createCheckout(@Valid @RequestBody CreateCheckoutRequest request) {
        return checkoutService.createCheckout(request);
    }

    /** Consulta o checkout mais recente de um pacote. */
    @GetMapping("/packages/{packageId}")
    public CreateCheckoutResponse findByPackage(@PathVariable("packageId") Long packageId) {
        return checkoutService.findCheckoutByPackage(packageId);
    }

    /** Solicita novo envio da entrega de uma compra. */
    @PostMapping("/{purchaseId}/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendDelivery(@PathVariable("purchaseId") Long purchaseId) {
        premiumDeliveryService.forceResend(purchaseId);
    }

    /** Ativa um checkout de teste temporário para um produto. */
    @PostMapping("/temporary")
    public TemporaryCheckoutResponse activateTemporary(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody TemporaryCheckoutRequest request) {
        temporaryCheckoutAdminAuthorizer.authorize(authorization);
        return temporaryCheckoutService.activate(request);
    }

    /** Consulta o estado do checkout temporário e aplica expiração quando necessária. */
    @GetMapping("/temporary/{productKey}")
    public TemporaryCheckoutResponse temporaryStatus(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("productKey") String productKey) {
        temporaryCheckoutAdminAuthorizer.authorize(authorization);
        return temporaryCheckoutService.status(productKey);
    }

    /** Restaura manualmente o checkout comercial. */
    @PostMapping("/temporary/{productKey}/restore")
    public TemporaryCheckoutResponse restoreTemporary(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable("productKey") String productKey) {
        temporaryCheckoutAdminAuthorizer.authorize(authorization);
        return temporaryCheckoutService.restore(productKey);
    }

    /** Redireciona para o checkout de teste vigente ou para o comercial após a expiração. */
    @GetMapping("/temporary/{productKey}/redirect")
    public ResponseEntity<Void> redirectTemporary(@PathVariable("productKey") String productKey) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(temporaryCheckoutService.resolveDestination(productKey))
                .build();
    }
}
