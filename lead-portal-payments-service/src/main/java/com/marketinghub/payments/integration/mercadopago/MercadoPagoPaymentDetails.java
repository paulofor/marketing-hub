package com.marketinghub.payments.integration.mercadopago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record MercadoPagoPaymentDetails(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String description,
        String email,
        Instant dateApproved,
        Map<String, Object> metadata,
        String rawPayload
) {
}
