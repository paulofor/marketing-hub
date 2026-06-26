package com.marketinghub.pipelines.oprm.nichocnae.v3.sourcesearcher.service.completeStageExecution;

/** Request de conclusão da etapa source-searcher reportada pelo executor. */
public record SourceSearcherCompletionRequest(String outputPayload, String nextStageCode) {}
