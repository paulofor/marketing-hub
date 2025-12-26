package com.marketinghub.payments.controller;

import com.marketinghub.payments.dto.CreateCheckoutRequest;
import com.marketinghub.payments.dto.CreateCheckoutResponse;
import com.marketinghub.payments.service.CheckoutService;
import com.marketinghub.payments.service.DeliveryService;
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
    private final DeliveryService deliveryService;

    public CheckoutController(CheckoutService checkoutService, DeliveryService deliveryService) {
        this.checkoutService = checkoutService;
        this.deliveryService = deliveryService;
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
        deliveryService.deliver(purchaseId);
    }
}
