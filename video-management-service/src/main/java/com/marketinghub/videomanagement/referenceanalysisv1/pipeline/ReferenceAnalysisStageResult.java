package com.marketinghub.videomanagement.referenceanalysisv1.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/** Resultado funcional e técnico completo reportado pelo executor ao backend. */
public record ReferenceAnalysisStageResult(
        String summaryMarkdown,
        JsonNode output,
        JsonNode artifacts,
        JsonNode rawRequest,
        JsonNode rawResponse,
        String model,
        Long inputTokens,
        Long cachedInputTokens,
        Long outputTokens,
        BigDecimal costUsd,
        String decision) {
}
