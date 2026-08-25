package com.marketinghub.agenttask;

import java.util.List;

/** Responsabilidade: transportar uma execução local real já concluída para a auditoria BPM. */
public record ImportedCompletedAgentTask(
    String assignedAgentKey,
    String requestedByName,
    String title,
    String description,
    String sourceReference,
    Long processDefinitionId,
    String processActivityId,
    String resultJson,
    String evidenceJson,
    List<AgentTaskModelUsageRequest> modelUsages) {}
