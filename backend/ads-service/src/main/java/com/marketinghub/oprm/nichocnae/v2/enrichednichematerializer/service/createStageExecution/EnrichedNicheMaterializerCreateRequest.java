package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.createStageExecution;

/** Contrato de escrita para criar pendência do materializador de nicho enriquecido calculado pelo executor externo. */
public record EnrichedNicheMaterializerCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
