package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa source-fetcher. */
public record SourceFetcherCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
