package com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.pending;

/** Item pendente entregue ao executor para a etapa persona-routine-materializer. */
public record PersonaRoutineMaterializerPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
