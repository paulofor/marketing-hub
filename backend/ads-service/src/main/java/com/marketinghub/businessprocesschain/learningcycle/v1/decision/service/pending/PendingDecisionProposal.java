package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.pending;

import com.fasterxml.jackson.databind.JsonNode;

/** Responsabilidade: transportar a execução reservada e o contexto oficial para Atena. */
public record PendingDecisionProposal(
    Long proposalId,
    String leaseToken,
    Long cycleId,
    long cycleRevision,
    Long activityDefinitionId,
    JsonNode context) {}
