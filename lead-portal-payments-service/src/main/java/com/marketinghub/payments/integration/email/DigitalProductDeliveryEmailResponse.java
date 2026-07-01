package com.marketinghub.payments.integration.email;

/**
 * Resposta do email-service para o envio de entrega pós-compra de produto digital.
 */
public record DigitalProductDeliveryEmailResponse(
        String requestId,
        String status,
        String message
) {
}
