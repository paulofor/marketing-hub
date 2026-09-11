package com.marketinghub.businessprocess.execution.service.recentExecutions;

/** Responsabilidade: entregar o prompt persistido integral de uma tarefa BPM identificada. */
public record BusinessProcessTaskPromptAuditResponse(
    Long taskId,
    String sourceReference,
    String promptSent,
    String agentPromptPart,
    String activityPromptPart) {}
