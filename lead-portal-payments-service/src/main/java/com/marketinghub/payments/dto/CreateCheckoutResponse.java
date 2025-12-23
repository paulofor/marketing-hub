package com.marketinghub.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCheckoutResponse(
        Long purchaseId,
        Long packageId,
        String preferenceId,
        String checkoutUrl,
        String status,
        BigDecimal amount,
        String currency,
        Instant expiresAt,
        String statementDescriptor
) {
}
