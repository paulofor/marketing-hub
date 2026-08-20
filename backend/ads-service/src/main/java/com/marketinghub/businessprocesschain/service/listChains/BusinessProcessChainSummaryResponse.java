package com.marketinghub.businessprocesschain.service.listChains;

import java.time.Instant;

/** Resposta resumida de uma cadeia de processos disponível no catálogo. */
public record BusinessProcessChainSummaryResponse(
    Long id,
    String chainCode,
    String name,
    String purpose,
    String outcomeDescription,
    String primaryMetric,
    Integer versionNumber,
    String status,
    Integer processCount,
    Instant publishedAt) {}
