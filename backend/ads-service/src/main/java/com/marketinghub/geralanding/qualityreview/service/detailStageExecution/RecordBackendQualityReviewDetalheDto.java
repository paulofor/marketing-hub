package com.marketinghub.geralanding.qualityreview.service.detailStageExecution;

import java.math.BigDecimal;
import java.time.Instant;

/** Detalhe completo de uma execução da revisão de qualidade da landing. */
public record RecordBackendQualityReviewDetalheDto(
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
        String qualityReviewAudit,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd
) {
    /** Mantém o contrato imutável do detalhe de execução. */
    public RecordBackendQualityReviewDetalheDto {}
}
