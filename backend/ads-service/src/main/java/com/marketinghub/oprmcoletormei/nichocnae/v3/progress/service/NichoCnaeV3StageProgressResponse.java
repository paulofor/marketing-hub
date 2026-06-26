package com.marketinghub.oprmcoletormei.nichocnae.v3.progress.service;

import java.time.Instant;

/** Representa o progresso de uma etapa do pipeline NichoCNAE v3 para a tela administrativa. */
public record NichoCnaeV3StageProgressResponse(
        Long stageExecutionId,
        String stageCode,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String errorMessage,
        String inputPayload,
        String outputPayload) {}
