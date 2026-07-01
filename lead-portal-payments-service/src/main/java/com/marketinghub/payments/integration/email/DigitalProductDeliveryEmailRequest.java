package com.marketinghub.payments.integration.email;

/**
 * Contrato enviado ao email-service para disparar a entrega pós-compra de produto digital.
 */
public record DigitalProductDeliveryEmailRequest(
        String to,
        String buyerName,
        String productName,
        String deliveryPageUrl,
        String downloadUrl,
        String paymentId,
        String externalReference
) {
}
