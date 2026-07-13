package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

import java.util.Map;

/**
 * Carrega os dados persistidos pelo backend para execução de uma etapa.
 */
public record StageContext(
        String jobId,
        String executionId,
        String experimentCode,
        String productIdea,
        String scientificQuestion,
        Map<String, Object> input,
        String callbackUrl) {
}
