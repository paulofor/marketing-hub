package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato recebido do executor ao concluir a etapa knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorCompletionRequest(
        Integer knowledgeVersion,
        Integer validatedFactCount,
        Integer acceptedSourceCount,
        Integer rejectedSourceCount,
        String outputPayload,
        String nextStageCode,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public KnowledgeAccumulatorCompletionRequest(
            Integer knowledgeVersion,
            Integer validatedFactCount,
            Integer acceptedSourceCount,
            Integer rejectedSourceCount,
            String outputPayload,
            String nextStageCode) {
        this(knowledgeVersion, validatedFactCount, acceptedSourceCount, rejectedSourceCount, outputPayload, nextStageCode, List.of());
    }
}
