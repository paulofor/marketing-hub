package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.createStageExecution;

/** Contrato devolvido após o backend gravar a pendência knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorCreateResponse(String stageExecutionId, String status, String stageCode) {}
