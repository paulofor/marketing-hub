package com.marketinghub.leadportal.payments;

public record LeadPortalCheckoutResponse(
        Long purchaseId,
        Long packageId,
        String preferenceId,
        String checkoutUrl,
        String status
) {
}
