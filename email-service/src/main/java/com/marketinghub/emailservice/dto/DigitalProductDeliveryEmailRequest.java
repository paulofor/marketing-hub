package com.marketinghub.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados necessários para enviar a entrega pós-compra de um produto digital.
 */
public record DigitalProductDeliveryEmailRequest(
        @Email(message = "Destinatário inválido")
        @NotBlank(message = "Destinatário é obrigatório")
        String to,
        String buyerName,
        @NotBlank(message = "Nome do produto é obrigatório")
        String productName,
        @NotBlank(message = "Página de entrega é obrigatória")
        String deliveryPageUrl,
        String downloadUrl,
        @NotBlank(message = "PaymentId é obrigatório")
        String paymentId,
        @NotBlank(message = "ExternalReference é obrigatória")
        String externalReference
) {
}
