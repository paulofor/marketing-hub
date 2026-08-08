package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeAgentReviewStatus;
import java.math.BigDecimal;

/** Responsabilidade: receber o parecer estruturado e a auditoria bruta do agente especialista. */
public record CreativeAgentReviewResultRequest(
    CreativeAgentReviewStatus decision,
    Integer attentionScore,
    Integer clarityScore,
    Integer desireScore,
    Integer credibilityScore,
    Integer actionScore,
    String summary,
    String issuesJson,
    String recommendationsJson,
    String model,
    String requestJson,
    String responseJson,
    Integer inputTokens,
    Integer outputTokens,
    BigDecimal costUsd,
    String error) {}
