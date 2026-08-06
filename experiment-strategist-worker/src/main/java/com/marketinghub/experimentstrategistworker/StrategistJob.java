package com.marketinghub.experimentstrategistworker;

/** Responsabilidade: representar a pesquisa reservada no backend. */
public record StrategistJob(
    Long id,
    Long commercialPlanId,
    String status,
    String authorityMode,
    String researchQuestion,
    String evidenceSnapshot) {}
