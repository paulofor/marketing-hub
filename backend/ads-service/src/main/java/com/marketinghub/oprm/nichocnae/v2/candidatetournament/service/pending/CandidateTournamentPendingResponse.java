package com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.pending;

/** Contrato entregue ao executor com a pendência da etapa candidate-tournament do NichoCNAE v2. */
public record CandidateTournamentPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        String cnaeDescription,
        Long researchCycleId,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
