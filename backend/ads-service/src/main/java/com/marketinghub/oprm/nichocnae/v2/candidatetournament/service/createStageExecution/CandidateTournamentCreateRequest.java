package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.createStageExecution;

/** Contrato enviado pelo executor para registrar uma pendência da etapa candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
