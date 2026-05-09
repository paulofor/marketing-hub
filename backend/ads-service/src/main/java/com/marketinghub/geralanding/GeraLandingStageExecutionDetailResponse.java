package com.marketinghub.geralanding;

import java.math.BigDecimal;
import java.time.Instant;

public record GeraLandingStageExecutionDetailResponse(
        String idJob,
        Long experimentId,
        String stageCode,
        Instant executionRequestedAt,
        Instant createdAt,
        Instant processingStartedAt,
        Instant completedAt,
        String promptTemplateId,
        String promptContent,
        String prompt,
        String openAiRequestBody,
        String openAiModel,
        String schemaJson,
        String promptMarkdownContent,
        String status,
        String openAiJobId,
        String modelResponse,
        String provisionalHtml,
        String errorMessage,
        String errorDetail,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
