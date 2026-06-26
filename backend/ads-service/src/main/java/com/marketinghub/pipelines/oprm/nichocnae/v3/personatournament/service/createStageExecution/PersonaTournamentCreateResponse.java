package com.marketinghub.pipelines.oprm.nichocnae.v3.personatournament.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa persona-tournament. */
public record PersonaTournamentCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
