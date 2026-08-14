package com.marketinghub.agentlearning.v1;

import java.time.Instant;

/** Visão auditável de uma skill candidata de agente. */
public record SkillCandidateResponse(
    Long id,
    Long experimentId,
    String agentKey,
    String skillKey,
    String baselineVersion,
    String candidateVersion,
    String content,
    String diffSummary,
    String provenanceJson,
    String safetyDecision,
    String safetyEvidence,
    String status,
    int monitoredCases,
    int approvedCases,
    Instant promotedAt,
    Instant rolledBackAt,
    String rollbackReason,
    Instant createdAt,
    Instant updatedAt) {}
