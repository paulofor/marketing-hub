package com.marketinghub.agenttask;

import java.util.List;

/** Responsabilidade: reconstruir uma falha de agente com os dados auditáveis da própria tarefa. */
public record AgentTaskFailureAuditResponse(
    String readiness,
    String intendedWork,
    String sourceReference,
    String processCode,
    String activityId,
    String activityName,
    String authorityPolicy,
    String accessedEvidenceJson,
    String producedOutputJson,
    String error,
    List<String> missingEvidence) {}
