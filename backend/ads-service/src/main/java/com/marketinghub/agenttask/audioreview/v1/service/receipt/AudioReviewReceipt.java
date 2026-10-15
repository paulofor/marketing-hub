package com.marketinghub.agenttask.audioreview.v1.service.receipt;

/** Transporta o contrato auditivo imutável desta operação da tarefa. */
public record AudioReviewReceipt(
    Long id,
    Long assetId,
    String videoSha256,
    String audioSha256,
    String status,
    boolean mayInvoke,
    java.math.BigDecimal reservedUsd,
    java.math.BigDecimal estimatedUsd,
    com.fasterxml.jackson.databind.JsonNode assessment,
    String error) {}
