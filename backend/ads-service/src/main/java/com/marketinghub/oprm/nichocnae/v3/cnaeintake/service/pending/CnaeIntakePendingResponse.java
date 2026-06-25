package com.marketinghub.oprm.nichocnae.v3.cnaeintake.service.pending;

/** Item pendente entregue ao executor para a etapa cnae-intake. */
public record CnaeIntakePendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
