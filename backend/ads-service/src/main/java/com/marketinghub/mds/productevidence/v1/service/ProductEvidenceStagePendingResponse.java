package com.marketinghub.mds.productevidence.v1.service;

import java.util.Map;

/** Contrato de pendência entregue ao scientific-research-worker para processamento de etapa. */
public record ProductEvidenceStagePendingResponse(
    String jobId,
    String executionId,
    String experimentCode,
    String productIdea,
    String scientificQuestion,
    Map<String, Object> input,
    String callbackUrl) {}
