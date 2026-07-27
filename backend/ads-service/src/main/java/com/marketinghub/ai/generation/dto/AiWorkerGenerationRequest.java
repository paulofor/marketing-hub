package com.marketinghub.ai.generation.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

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
