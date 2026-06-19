package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa candidate-generator do NichoCNAE v2. */
public record CandidateGeneratorCompletionRequest(
        String qualityStatus,
        String requestedAction,
        String outputPayload,
        String nextStageCode) {
    /** Mantém compatibilidade com chamadas que ainda não enviam a próxima etapa decidida pelo executor. */
    public CandidateGeneratorCompletionRequest(String qualityStatus, String requestedAction, String outputPayload) {
        this(qualityStatus, requestedAction, outputPayload, null);
    }
}
