package com.marketinghub.oprm.nichocnae.v3.sourcefetcher.service.pending;

/** Item pendente entregue ao executor para a etapa source-fetcher. */
public record SourceFetcherPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
