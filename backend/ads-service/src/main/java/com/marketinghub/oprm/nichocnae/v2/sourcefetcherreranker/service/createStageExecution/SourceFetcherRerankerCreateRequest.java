package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.createStageExecution;

/** Contrato enviado pelo executor para registrar uma pendência da etapa source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
