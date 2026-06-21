package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato recebido do executor ao concluir a etapa candidate-generator do NichoCNAE v2. */
public record CandidateGeneratorCompletionRequest(
        String qualityStatus,
        String requestedAction,
        String outputPayload,
        String nextStageCode,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public CandidateGeneratorCompletionRequest(
            String qualityStatus, String requestedAction, String outputPayload, String nextStageCode) {
        this(qualityStatus, requestedAction, outputPayload, nextStageCode, List.of());
    }

    /** Mantém compatibilidade com chamadas que ainda não enviam a próxima etapa decidida pelo executor. */
    public CandidateGeneratorCompletionRequest(String qualityStatus, String requestedAction, String outputPayload) {
        this(qualityStatus, requestedAction, outputPayload, null, List.of());
    }
}
