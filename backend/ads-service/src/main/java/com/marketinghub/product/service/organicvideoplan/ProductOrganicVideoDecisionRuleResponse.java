package com.marketinghub.product.service.organicvideoplan;

/** Responsabilidade: explicar como interpretar os sinais dos vídeos orgânicos publicados. */
public record ProductOrganicVideoDecisionRuleResponse(
    String signal,
    String condition,
    String decision,
    String commercialReason) {}
