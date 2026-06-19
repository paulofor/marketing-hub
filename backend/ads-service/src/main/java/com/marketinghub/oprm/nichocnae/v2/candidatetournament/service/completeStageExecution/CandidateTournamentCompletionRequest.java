package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentCompletionRequest(
        String tournamentDecision,
        Integer candidateCount,
        Integer finalistCount,
        String outputPayload,
        String nextStageCode) {}
