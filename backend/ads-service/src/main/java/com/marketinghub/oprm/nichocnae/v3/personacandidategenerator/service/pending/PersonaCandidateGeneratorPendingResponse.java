package com.marketinghub.oprm.nichocnae.v3.personacandidategenerator.service.pending;

/** Item pendente entregue ao executor para a etapa persona-candidate-generator. */
public record PersonaCandidateGeneratorPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
