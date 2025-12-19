package com.marketinghub.payments.dto;

public record CreateCheckoutResponse(
        Long purchaseId,
        Long packageId,
        String preferenceId,
        String checkoutUrl,
        String status
) {
}
