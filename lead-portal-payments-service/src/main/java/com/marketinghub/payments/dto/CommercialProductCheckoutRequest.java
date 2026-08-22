package com.marketinghub.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Recebe o contrato comercial validado pelo Marketing Hub para criar um checkout de produto. */
public record CommercialProductCheckoutRequest(
        @NotBlank String productKey,
        @NotBlank String productName,
        @NotNull Long productId,
        @NotNull Long experimentId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String deliveryPageUrl) {}
