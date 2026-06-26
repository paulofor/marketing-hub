package com.marketinghub.pipelines.oprm.nichocnae.v3.personatournament.service.completeStageExecution;

/** Request de conclusão da etapa persona-tournament reportada pelo executor. */
public record PersonaTournamentCompletionRequest(String outputPayload, String nextStageCode) {}
