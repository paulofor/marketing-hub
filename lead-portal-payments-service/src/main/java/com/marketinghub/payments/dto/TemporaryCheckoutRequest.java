package com.marketinghub.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Dados administrativos para ativar um checkout temporário de teste. */
public record TemporaryCheckoutRequest(
        @NotBlank String productKey,
        @NotBlank String productName,
        @NotNull @DecimalMin("0.01") BigDecimal testAmount,
        @NotBlank String commercialCheckoutUrl,
        @NotNull Integer durationMinutes) {}
