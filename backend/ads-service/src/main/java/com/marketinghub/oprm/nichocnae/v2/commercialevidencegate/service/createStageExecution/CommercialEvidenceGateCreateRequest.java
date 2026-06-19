package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.createStageExecution;

/** Contrato de escrita para criar pendência do gate comercial calculado pelo executor externo. */
public record CommercialEvidenceGateCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
