package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.createStageExecution;

/** Contrato devolvido após o backend gravar a pendência candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentCreateResponse(String stageExecutionId, String status, String stageCode) {}
