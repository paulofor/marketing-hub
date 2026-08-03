package com.marketinghub.growthoperatorworker;

/** Responsabilidade: representar uma pendencia recebida do backend. */
public record GrowthOperatorJob(
    Long id,
    Long commercialPlanId,
    Integer weekNumber,
    String authorityMode,
    String objective,
    String blocker,
    String evidenceSnapshot) {}
