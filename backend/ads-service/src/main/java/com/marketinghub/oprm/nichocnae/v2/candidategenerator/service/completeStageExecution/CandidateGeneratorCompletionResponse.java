package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution;

/** Contrato retornado ao executor após registrar a conclusão da etapa candidate-generator do NichoCNAE v2. */
public record CandidateGeneratorCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        Boolean materializationEnabled) {}
