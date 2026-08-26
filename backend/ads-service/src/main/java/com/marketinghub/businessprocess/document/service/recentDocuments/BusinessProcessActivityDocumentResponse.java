package com.marketinghub.businessprocess.document.service.recentDocuments;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de leitura de um documento produzido por uma execução de atividade BPM. */
public record BusinessProcessActivityDocumentResponse(
    Long taskId,
    String title,
    String sourceReference,
    String assignedAgentKey,
    String assignedAgentNickname,
    String resultJson,
    String evidenceJson,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant startedAt,
    Instant finishedAt,
    String modelCode,
    String reasoningEffort,
    String productInternalName,
    String promptSent) {}
