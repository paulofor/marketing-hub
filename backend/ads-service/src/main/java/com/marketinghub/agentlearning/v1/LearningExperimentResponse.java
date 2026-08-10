package com.marketinghub.agentlearning.v1;

/** Resumo auditável da decisão de aprendizado. */
public record LearningExperimentResponse(
    Long id,
    String agentKey,
    String candidateVersion,
    String baselineVersion,
    String status,
    String decisionEvidence) {}
