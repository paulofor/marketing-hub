package com.marketinghub.ai.generation.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AiWorkerGenerationRequest {
    String domain;
    String referenceId;
    String prompt;
    String rawResponse;
    String model;
    Integer inputTokens;
    Integer outputTokens;
    BigDecimal costUsd;
}
