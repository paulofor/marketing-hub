package com.marketinghub.agentmemory.service.retrieveMemory;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de uma memória recuperada com sua procedência e estado de confiança. */
public record MemoryResponse(
    Long id,
    String status,
    String specialty,
    String content,
    String evidence,
    String sourceReference,
    String sourceExecutionId,
    BigDecimal confidence,
    Instant validUntil,
    long retrievalCount,
    Instant createdAt) {}
