package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.creative.CreativeAgentReviewStatus;
import java.math.BigDecimal;

/** Responsabilidade: transportar o histórico leve de um criativo revisado por Têmis. */
public record TemisVisualLearningCreativeHistory(
    Long creativeId,
    Long experimentId,
    Integer versionNumber,
    String format,
    BigDecimal costUsd,
    CreativeAgentReviewStatus reviewStatus,
    String reviewJson,
    String reviewRequestJson,
    String reviewResponseJson,
    String improvementJson) {}
