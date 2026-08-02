package com.marketinghub.winningadlibrary.service.listWinningAds;

import java.time.Instant;

/** Resposta com um anúncio vencedor pronto para análise e reuso comercial. */
public record WinningAdResponse(
    Long id,
    String productSlug,
    String productName,
    String niche,
    String funnelStage,
    String channel,
    String format,
    String winningStatus,
    Integer score,
    String hook,
    String primaryText,
    String creativeBrief,
    String offerAngle,
    String proofSignal,
    String metricSnapshot,
    String learning,
    String nextAction,
    String sourceReference,
    Instant updatedAt) {}
