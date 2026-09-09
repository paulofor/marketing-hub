package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.get;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Responsabilidade: apresentar a proposta comercial e seu estado para revisão humana. */
public record DecisionProposalResponse(
    Long id,
    Long cycleId,
    long cycleRevision,
    String status,
    String agentKey,
    String agentName,
    Long agentId,
    boolean automaticExecutionEnabled,
    Long activityDefinitionId,
    JsonNode proposal,
    String operatorName,
    String error,
    Instant createdAt,
    Instant finishedAt,
    Instant approvedAt,
    Long approvedEventId) {}
