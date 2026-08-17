package com.marketinghub.agentlearning.v1;

/** Responsabilidade: registrar uma falha técnica do consolidador sem perder a amostra congelada. */
public record TemisVisualLearningFailureRequest(String producerExecutionId, String error) {}
