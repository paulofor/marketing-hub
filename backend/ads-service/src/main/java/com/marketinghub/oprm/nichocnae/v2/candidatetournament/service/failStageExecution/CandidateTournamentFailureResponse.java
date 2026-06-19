package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.failStageExecution;

/** Contrato devolvido após o backend registrar falha candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
