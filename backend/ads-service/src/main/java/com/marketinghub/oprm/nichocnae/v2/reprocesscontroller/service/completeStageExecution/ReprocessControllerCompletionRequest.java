package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato de escrita para registrar plano de retry ou reprocessamento decidido pelo executor externo. */
public record ReprocessControllerCompletionRequest(String executionMode, String rewindToStage, Integer knowledgeVersionTo, String nextStageCode, String outputPayload,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public ReprocessControllerCompletionRequest(
            String executionMode, String rewindToStage, Integer knowledgeVersionTo, String nextStageCode, String outputPayload) {
        this(executionMode, rewindToStage, knowledgeVersionTo, nextStageCode, outputPayload, List.of());
    }
}
