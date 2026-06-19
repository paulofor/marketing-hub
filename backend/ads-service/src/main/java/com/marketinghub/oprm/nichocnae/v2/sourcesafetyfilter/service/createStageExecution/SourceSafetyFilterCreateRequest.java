package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.createStageExecution;

/** Contrato enviado pelo executor para registrar uma pendência da etapa source-safety-filter do NichoCNAE v2. */
public record SourceSafetyFilterCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
