package com.marketinghub.consumerexpectation.v1.service.learning;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Projeta evidência comercial já conciliada, sem atribuir causalidade à surpresa. */
public record ExpectationLearningResponse(
    String contractVersion,
    Long productId,
    Long cycleId,
    Long experimentId,
    String productVersion,
    Long previousCycleId,
    String status,
    Long measurementEventId,
    Instant observedAt,
    JsonNode metrics,
    String sourceFingerprint,
    String nextOwner,
    String guidance,
    String cycleUrl,
    boolean causalConclusion,
    boolean commercialAuthorization) {}
