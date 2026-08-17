package com.marketinghub.agentlearning.v1;

import com.fasterxml.jackson.databind.JsonNode;

/** Responsabilidade: entregar uma amostra congelada ao consolidador independente de Têmis. */
public record TemisVisualLearningPendingDto(
    Long runId,
    String contextKey,
    String baselineVersion,
    String candidateVersion,
    JsonNode input,
    String producerExecutionId) {}
