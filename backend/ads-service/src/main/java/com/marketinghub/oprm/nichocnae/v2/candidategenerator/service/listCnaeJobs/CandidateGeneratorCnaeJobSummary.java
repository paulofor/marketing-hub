package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.listCnaeJobs;

import java.time.Instant;

/** Representa o resumo administrativo de um job v2 agrupado por jobId. */
public record CandidateGeneratorCnaeJobSummary(
        String jobId,
        String cnaeCode,
        String status,
        String currentStageCode,
        String lastStageCode,
        String lastStageStatus,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        Instant createdAt,
        Instant updatedAt) {}
