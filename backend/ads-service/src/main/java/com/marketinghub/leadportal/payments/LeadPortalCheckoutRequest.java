package com.marketinghub.leadportal.payments;

public record LeadPortalCheckoutRequest(
        Long packageId,
        String buyerEmail,
        String buyerName
) {
}
