package com.marketinghub.pipelines.oprm.nichocnae.v3.sourcesearcher.service.failStageExecution;

/** Request de falha da etapa source-searcher reportada pelo executor. */
public record SourceSearcherFailureRequest(String errorMessage) {}
