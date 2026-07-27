package com.marketinghub.ai.generation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

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
