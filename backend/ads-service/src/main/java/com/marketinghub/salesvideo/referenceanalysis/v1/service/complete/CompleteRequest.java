package com.marketinghub.salesvideo.referenceanalysis.v1.service.complete;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Contrato completo de sucesso retornado pelo executor ao backend. */
public record CompleteRequest(
    @NotBlank String producerExecutionId,
    @NotBlank String summaryMarkdown,
    @NotNull JsonNode output,
    @NotNull JsonNode artifacts,
    @NotNull JsonNode rawRequest,
    @NotNull JsonNode rawResponse,
    @NotBlank String model,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal costUsd,
    @NotBlank String decision) {}
