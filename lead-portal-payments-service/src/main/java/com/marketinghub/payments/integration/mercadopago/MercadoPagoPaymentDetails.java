package com.marketinghub.payments.integration.mercadopago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Representa os dados normalizados de um pagamento retornado pelo Mercado Pago.
 */
public record MercadoPagoPaymentDetails(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String description,
        String email,
        String externalReference,
        Instant dateApproved,
        Map<String, Object> metadata,
        String rawPayload
) {
}
