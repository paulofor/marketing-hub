package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.completeStageExecution;

/** Contrato retornado ao executor após registrar a conclusão do filtro de segurança de fontes. */
public record SourceSafetyFilterCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        Integer allowedUrlCount,
        Integer rejectedUrlCount) {}
