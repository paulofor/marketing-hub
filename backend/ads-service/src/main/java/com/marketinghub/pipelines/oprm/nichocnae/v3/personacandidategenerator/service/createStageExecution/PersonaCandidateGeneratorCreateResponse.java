package com.marketinghub.pipelines.oprm.nichocnae.v3.personacandidategenerator.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa persona-candidate-generator. */
public record PersonaCandidateGeneratorCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
