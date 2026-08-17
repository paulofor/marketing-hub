package com.marketinghub.agentlearning.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: receber o replay independente e a candidata de playbook visual. */
public record TemisVisualLearningResultRequest(
    String producerExecutionId,
    List<String> rules,
    List<String> avoid,
    BigDecimal baselineReplayScore,
    BigDecimal candidateReplayScore,
    BigDecimal baselineHoldoutScore,
    BigDecimal candidateHoldoutScore,
    boolean regressionPassed,
    boolean localValidationPassed,
    boolean externalProviderCalled,
    boolean spendingAuthorized,
    boolean publicationPerformed,
    JsonNode caseAssessments,
    String requestJson,
    String responseJson) {}
