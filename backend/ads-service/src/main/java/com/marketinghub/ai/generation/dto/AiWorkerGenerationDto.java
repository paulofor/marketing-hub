package com.marketinghub.ai.generation.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class AiWorkerGenerationDto {
    Long id;
    String domain;
    String referenceId;
    String model;
    String prompt;
    String rawResponse;
    Integer inputTokens;
    Integer outputTokens;
    BigDecimal costUsd;
    Instant createdAt;
}
