package com.marketinghub.oprm.nichocnae.v3.sourcefetcher.service.failStageExecution;

/** Request de falha da etapa source-fetcher reportada pelo executor. */
public record SourceFetcherFailureRequest(String errorMessage) {}
