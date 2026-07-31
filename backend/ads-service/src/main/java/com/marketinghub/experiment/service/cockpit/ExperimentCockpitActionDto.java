package com.marketinghub.experiment.service.cockpit;

/** Ação comercial recomendada pelo cockpit para avançar o experimento. */
public record ExperimentCockpitActionDto(
    String code, String label, String rationale, String targetRoute) {}
