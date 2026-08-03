package com.marketinghub.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Estado público e administrativo do checkout temporário. */
public record TemporaryCheckoutResponse(
        String productKey,
        String productName,
        String redirectUrl,
        String temporaryCheckoutUrl,
        String commercialCheckoutUrl,
        BigDecimal testAmount,
        String status,
        Instant activatedAt,
        Instant expiresAt) {}
