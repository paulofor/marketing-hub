package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.pending;

/** Contrato de leitura de pendência da etapa commercial-evidence-gate para o executor OPRM. */
public record CommercialEvidenceGatePendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        String cnaeDescription,
        Long researchCycleId,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
