package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending;

/** Contrato de fila interna entregue ao executor para processar a etapa candidate-generator do NichoCNAE v2. */
public record CandidateGeneratorPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        String cnaeDescription,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled) {}
