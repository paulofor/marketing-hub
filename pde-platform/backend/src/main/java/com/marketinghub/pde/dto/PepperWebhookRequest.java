package com.marketinghub.pde.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Contrato mínimo do webhook Pepper para liberar acesso após compra aprovada. */
public record PepperWebhookRequest(
        @NotBlank String productSlug,
        @Email @NotBlank String buyerEmail,
        @NotBlank String transactionId,
        @NotBlank String status
) {}
