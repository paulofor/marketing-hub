package com.marketinghub.financialagentworker;

/** Responsabilidade: representar uma conciliacao financeira recebida do backend. */
public record FinancialAgentJob(
    Long id,
    Long commercialPlanId,
    String status,
    String authorityMode,
    Integer commercialPlanVersion,
    Long agentTaskId,
    String projectionRequest,
    String financialSnapshot) {}
