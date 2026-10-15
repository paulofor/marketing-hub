package com.marketinghub.agenttask.audioreview.v1.service.reserve;

/** Transporta o contrato auditivo imutável desta operação da tarefa. */
public record AudioReviewReserveRequest(
    Long assetId,
    String videoSha256,
    String audioSha256,
    com.fasterxml.jackson.databind.JsonNode request) {}
