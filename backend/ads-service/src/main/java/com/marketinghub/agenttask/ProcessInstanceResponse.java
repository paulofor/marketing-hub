package com.marketinghub.agenttask;

import java.util.List;

/** Responsabilidade: expor uma execução BPM e suas tarefas para diagnóstico operacional. */
public record ProcessInstanceResponse(
    Long processDefinitionId,
    String processCode,
    Integer processVersionNumber,
    String sourceReference,
    List<ProcessInstanceTaskResponse> tasks,
    List<ProcessInstanceTaskResponse> supersededLegacyTasks) {}
