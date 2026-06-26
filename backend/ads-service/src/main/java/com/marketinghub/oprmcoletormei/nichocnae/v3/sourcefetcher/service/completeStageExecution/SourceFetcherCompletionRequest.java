package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.completeStageExecution;

/** Request de conclusão da etapa source-fetcher reportada pelo executor. */
public record SourceFetcherCompletionRequest(String outputPayload, String nextStageCode) {}
