package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.completeStageExecution;

import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import java.util.List;

/** Contrato recebido do executor ao concluir a etapa candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentCompletionRequest(
        String tournamentDecision,
        Integer candidateCount,
        Integer finalistCount,
        String outputPayload,
        String nextStageCode,
        List<OpenAiInteractionAuditRequest> openAiInteractions) {
    /** Mantém compatibilidade com chamadas que ainda não enviam auditoria OpenAI estruturada. */
    public CandidateTournamentCompletionRequest(
            String tournamentDecision, Integer candidateCount, Integer finalistCount, String outputPayload, String nextStageCode) {
        this(tournamentDecision, candidateCount, finalistCount, outputPayload, nextStageCode, List.of());
    }
}
