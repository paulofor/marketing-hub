package com.marketinghub.creative.service.videoreview;

/** Consolida a fila no escopo consultado sem equiparar todo rascunho a uma aprovação pendente. */
public record VideoReviewSummary(
    Long productId,
    Long experimentId,
    long awaitingReviewCount,
    long blockedCount,
    long historicalCount,
    long approvedCount,
    long rejectedCount,
    long optionalReviewCount) {}
