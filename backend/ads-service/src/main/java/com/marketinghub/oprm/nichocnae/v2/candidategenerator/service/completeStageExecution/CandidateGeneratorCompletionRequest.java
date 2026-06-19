package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa candidate-generator do NichoCNAE v2. */
public record CandidateGeneratorCompletionRequest(
        String qualityStatus,
        String requestedAction,
        String outputPayload) {}
