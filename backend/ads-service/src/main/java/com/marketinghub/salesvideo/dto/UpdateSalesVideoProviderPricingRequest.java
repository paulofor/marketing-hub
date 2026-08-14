package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: receber a evidência oficial de preço pesquisada por Plutus. */
public record UpdateSalesVideoProviderPricingRequest(
    @DecimalMin("0.000001") BigDecimal amountUsd,
    @NotBlank String unit,
    @DecimalMin("0.0001") BigDecimal quantity,
    String resolution,
    Boolean includesAudio,
    @NotBlank String sourceUrl,
    @NotNull Instant observedAt,
    @NotBlank String status,
    String notes,
    String rawResponse,
    String researchModel) {}
