package com.marketinghub.pde.dto;

/** Retorna a reconciliação idempotente de um reembolso ou chargeback confirmado na Pepper. */
public record PepperRefundResponse(
        String productSlug,
        String transactionId,
        String paymentStatus,
        boolean newlyRecorded,
        boolean accessRevoked,
        String refundedAt) {}
