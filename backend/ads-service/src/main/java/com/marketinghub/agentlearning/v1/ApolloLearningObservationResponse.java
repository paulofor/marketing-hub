package com.marketinghub.agentlearning.v1;

/** Resultado auditável da incorporação de uma observação ao piloto de Apolo. */
public record ApolloLearningObservationResponse(
    Long observationId, int collectedCases, int requiredCases, Long experimentId, String status) {}
