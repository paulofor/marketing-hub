package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service.pending;

/** Item pendente entregue ao executor para a etapa source-searcher. */
public record SourceSearcherPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
