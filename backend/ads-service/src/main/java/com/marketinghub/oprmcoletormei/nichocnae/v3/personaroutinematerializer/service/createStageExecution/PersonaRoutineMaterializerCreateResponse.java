package com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa persona-routine-materializer. */
public record PersonaRoutineMaterializerCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
