package com.marketinghub.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Transporta ao PDE o pagamento já consultado na API autoritativa do Mercado Pago. */
public record PdePaymentEntitlementNotification(
        String paymentId,
        String paymentStatus,
        BigDecimal amount,
        String currency,
        String buyerEmail,
        String externalReference,
        Instant dateApproved,
        Map<String, Object> metadata) {}
