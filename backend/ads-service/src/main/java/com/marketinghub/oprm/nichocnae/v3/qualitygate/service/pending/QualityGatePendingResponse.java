package com.marketinghub.oprm.nichocnae.v3.qualitygate.service.pending;

/** Item pendente entregue ao executor para a etapa quality-gate. */
public record QualityGatePendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
