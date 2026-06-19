package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.completeStageExecution;

/** Contrato devolvido após o backend registrar a conclusão candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        String tournamentDecision,
        Integer candidateCount,
        Integer finalistCount) {}
