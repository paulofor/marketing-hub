package com.marketinghub.payments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados informados pelo comprador para receber a entrega digital por email.
 */
public record DigitalProductDeliveryEmailRetryRequest(
        @NotBlank(message = "PaymentId é obrigatório")
        String paymentId,
        @Email(message = "Email inválido")
        @NotBlank(message = "Email é obrigatório")
        String email,
        String name
) {
}
