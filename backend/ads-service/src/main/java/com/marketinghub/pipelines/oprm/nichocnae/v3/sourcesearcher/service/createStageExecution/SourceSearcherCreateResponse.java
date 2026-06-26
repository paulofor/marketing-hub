package com.marketinghub.pipelines.oprm.nichocnae.v3.sourcesearcher.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa source-searcher. */
public record SourceSearcherCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
