package com.marketinghub.payments.dto;

import java.math.BigDecimal;

/** Expõe o checkout comercial criado e sua correlação com produto e experimento. */
public record CommercialProductCheckoutResponse(
        String productKey,
        Long productId,
        Long experimentId,
        String preferenceId,
        String checkoutUrl,
        BigDecimal amount,
        String currency,
        String deliveryPageUrl) {}
