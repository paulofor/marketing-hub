package com.marketinghub.pde.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Representa o pagamento que o serviço oficial acabou de confirmar no Mercado Pago. */
public record MercadoPagoEntitlementRequest(
        @NotBlank String paymentId,
        @NotBlank String paymentStatus,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @Email @NotBlank String buyerEmail,
        @NotBlank String externalReference,
        Instant dateApproved,
        @NotNull Map<String, Object> metadata) {}
