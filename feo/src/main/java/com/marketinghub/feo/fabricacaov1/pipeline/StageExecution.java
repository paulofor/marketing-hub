package com.marketinghub.feo.fabricacaov1.pipeline;

import java.util.Map;

/**
 * Carrega uma execucao pendente recebida pelo endpoint pending do backend.
 */
public record StageExecution<I>(
        String jobId,
        String executionId,
        StageCode stageCode,
        I input,
        Map<String, Object> config) {
}
