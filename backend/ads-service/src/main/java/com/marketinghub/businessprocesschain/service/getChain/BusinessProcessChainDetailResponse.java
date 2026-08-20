package com.marketinghub.businessprocesschain.service.getChain;

import java.time.Instant;
import java.util.List;

/** Resposta detalhada da cadeia com seus processos na ordem de geração de valor. */
public record BusinessProcessChainDetailResponse(
    Long id,
    String chainCode,
    String name,
    String purpose,
    String outcomeDescription,
    String primaryMetric,
    Integer versionNumber,
    String status,
    Integer processCount,
    Instant createdAt,
    Instant publishedAt,
    List<BusinessProcessChainProcessResponse> processes) {}
