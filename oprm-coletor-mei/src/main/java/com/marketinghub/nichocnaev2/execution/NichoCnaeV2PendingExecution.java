package com.marketinghub.nichocnaev2.execution;

import java.util.Map;

/** Representa uma unidade pendente do backend NichoCNAE v2 consumida pelo agendador do executor. */
public record NichoCnaeV2PendingExecution(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        String cnaeDescription,
        Long researchCycleId,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload,
        Map<String, Object> rawPayload) {}
