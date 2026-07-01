package com.marketinghub.gerasalespage.v1.service;

import java.math.BigDecimal;
import java.time.Instant;

/** Snapshot de uma etapa usada para criar uma versao publicada da pagina de venda. */
public record GeraSalesPagePublicationStageResponse(
        String idJob,
        String stageCode,
        String status,
        Instant completedAt,
        String promptTemplateKey,
        String prompt,
        String promptMarkdownContent,
        String schemaJson,
        String openAiModel,
        String openAiRequestBody,
        String modelResponse,
        String rawResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {}
