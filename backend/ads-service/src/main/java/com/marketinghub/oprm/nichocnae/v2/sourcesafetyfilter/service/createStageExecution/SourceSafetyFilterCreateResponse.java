package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.createStageExecution;

/** Contrato retornado ao executor após o backend gravar a pendência source-safety-filter solicitada. */
public record SourceSafetyFilterCreateResponse(
        String stageExecutionId,
        String status,
        String stageCode) {}
