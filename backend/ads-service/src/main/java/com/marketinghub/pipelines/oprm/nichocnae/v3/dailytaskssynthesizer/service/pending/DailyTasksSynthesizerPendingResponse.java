package com.marketinghub.pipelines.oprm.nichocnae.v3.dailytaskssynthesizer.service.pending;

/** Item pendente entregue ao executor para a etapa daily-tasks-synthesizer. */
public record DailyTasksSynthesizerPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
