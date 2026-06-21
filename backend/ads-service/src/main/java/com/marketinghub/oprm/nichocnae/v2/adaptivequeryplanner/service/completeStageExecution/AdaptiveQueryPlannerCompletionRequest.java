package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato recebido do executor ao concluir a etapa adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerCompletionRequest(
        String planDecision,
        Integer plannedQueryCount,
        Integer reusedQueryCount,
        Integer skippedQueryCount,
        String outputPayload,
        String nextStageCode,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public AdaptiveQueryPlannerCompletionRequest(
            String planDecision,
            Integer plannedQueryCount,
            Integer reusedQueryCount,
            Integer skippedQueryCount,
            String outputPayload,
            String nextStageCode) {
        this(planDecision, plannedQueryCount, reusedQueryCount, skippedQueryCount, outputPayload, nextStageCode, List.of());
    }
}
