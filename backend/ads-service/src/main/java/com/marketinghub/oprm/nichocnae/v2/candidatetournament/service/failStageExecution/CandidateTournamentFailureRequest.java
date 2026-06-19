package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato recebido do executor para registrar falha da etapa candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentFailureRequest(
        OprmNichoCnaeV2FailureType failureType, String errorMessage, String inputPayload) {}
