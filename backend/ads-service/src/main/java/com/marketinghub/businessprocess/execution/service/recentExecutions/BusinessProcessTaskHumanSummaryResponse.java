package com.marketinghub.businessprocess.execution.service.recentExecutions;

import com.marketinghub.agenttask.AgentTaskHumanSummary;

/** Responsabilidade: transportar somente a síntese humana, preservando a auditoria sob demanda. */
public record BusinessProcessTaskHumanSummaryResponse(AgentTaskHumanSummary humanSummary) {}
