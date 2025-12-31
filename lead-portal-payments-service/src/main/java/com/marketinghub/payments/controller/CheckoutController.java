package com.marketinghub.payments.controller;

import com.marketinghub.payments.dto.CreateCheckoutRequest;
import com.marketinghub.payments.dto.CreateCheckoutResponse;
import com.marketinghub.payments.service.CheckoutService;
import com.marketinghub.payments.service.PremiumDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final PremiumDeliveryService premiumDeliveryService;

    public CheckoutController(CheckoutService checkoutService, PremiumDeliveryService premiumDeliveryService) {
        this.checkoutService = checkoutService;
        this.premiumDeliveryService = premiumDeliveryService;
    }

    @PostMapping("/checkout")
    public CreateCheckoutResponse createCheckout(@Valid @RequestBody CreateCheckoutRequest request) {
        return checkoutService.createCheckout(request);
    }

    @GetMapping("/packages/{packageId}")
    public CreateCheckoutResponse findByPackage(@PathVariable("packageId") Long packageId) {
        return checkoutService.findCheckoutByPackage(packageId);
    }

    @PostMapping("/{purchaseId}/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendDelivery(@PathVariable("purchaseId") Long purchaseId) {
        premiumDeliveryService.forceResend(purchaseId);
    }
}
