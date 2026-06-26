package com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.completeStageExecution;

/** Request de conclusão da etapa persona-candidate-generator reportada pelo executor. */
public record PersonaCandidateGeneratorCompletionRequest(String outputPayload, String nextStageCode) {}
