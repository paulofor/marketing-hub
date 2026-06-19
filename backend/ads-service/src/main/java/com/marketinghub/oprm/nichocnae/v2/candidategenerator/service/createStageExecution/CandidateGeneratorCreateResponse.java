package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.createStageExecution;

/** Contrato retornado ao frontend quando um novo job v2 é gravado para consumo do executor externo. */
public record CandidateGeneratorCreateResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String status) {}
