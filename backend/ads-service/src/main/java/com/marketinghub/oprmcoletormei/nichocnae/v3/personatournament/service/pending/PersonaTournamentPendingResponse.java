package com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.pending;

/** Item pendente entregue ao executor para a etapa persona-tournament. */
public record PersonaTournamentPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
