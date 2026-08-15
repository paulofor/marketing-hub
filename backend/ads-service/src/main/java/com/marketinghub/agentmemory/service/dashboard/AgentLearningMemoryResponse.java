package com.marketinghub.agentmemory.service.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato detalhado de uma memória com procedência e uso observável. */
public record AgentLearningMemoryResponse(
    Long id,
    String agentKey,
    String agentName,
    String tenantKey,
    String scopeType,
    String scopeId,
    String specialty,
    String content,
    String evidence,
    String sourceReference,
    String sourceExecutionId,
    String status,
    BigDecimal confidence,
    long retrievalCount,
    Instant lastRetrievedAt,
    Instant validUntil,
    Instant createdAt,
    Instant updatedAt) {}
