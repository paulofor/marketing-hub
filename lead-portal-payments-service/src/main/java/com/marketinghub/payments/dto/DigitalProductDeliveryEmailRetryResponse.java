package com.marketinghub.payments.dto;

/**
 * Resultado da tentativa de envio de email de entrega digital.
 */
public record DigitalProductDeliveryEmailRetryResponse(
        String status,
        String message
) {
}
