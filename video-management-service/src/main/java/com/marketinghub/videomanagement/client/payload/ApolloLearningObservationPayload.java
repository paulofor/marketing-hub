package com.marketinghub.videomanagement.client.payload;

import java.math.BigDecimal;

/** Contrato de uma observação sombra enviada ao aprendizado governado de Apolo. */
public record ApolloLearningObservationPayload(
        Long jobId, String scopeId, String baselineVersion, String candidateVersion,
        BigDecimal baselineScore, BigDecimal candidateScore, BigDecimal baselineCost,
        BigDecimal candidateCost, String comparisonJson, boolean providerCalled,
        boolean spendingAuthorized, boolean publicationPerformed) {}
