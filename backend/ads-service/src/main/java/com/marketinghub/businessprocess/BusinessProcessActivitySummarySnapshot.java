package com.marketinghub.businessprocess;

/** Responsabilidade: transportar a identidade mínima de uma atividade para calcular progresso. */
public record BusinessProcessActivitySummarySnapshot(Long processDefinitionId, String activityId) {}
