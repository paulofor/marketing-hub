package com.marketinghub.salesvideo.referenceanalysis.v1.service.fail;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Contrato de falha auditável retornado pelo executor. */
public record FailureRequest(
    @NotBlank String producerExecutionId,
    @NotBlank String error,
    JsonNode artifacts,
    JsonNode rawRequest,
    JsonNode rawResponse,
    String model,
    @PositiveOrZero Long inputTokens,
    @PositiveOrZero Long cachedInputTokens,
    @PositiveOrZero Long outputTokens,
    @DecimalMin("0.0") BigDecimal costUsd) {}
