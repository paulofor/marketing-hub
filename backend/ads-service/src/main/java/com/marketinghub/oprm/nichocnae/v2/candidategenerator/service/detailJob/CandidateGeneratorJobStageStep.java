package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.detailJob;

import java.time.Instant;

/** Representa uma etapa executada dentro de um job NichoCNAE v2 para relatório administrativo. */
public record CandidateGeneratorJobStageStep(
        String stageExecutionId,
        String stageCode,
        String status,
        String failureType,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload,
        String outputPayload,
        String errorMessage,
        String nextStageCode,
        Instant createdAt,
        Instant updatedAt) {}
