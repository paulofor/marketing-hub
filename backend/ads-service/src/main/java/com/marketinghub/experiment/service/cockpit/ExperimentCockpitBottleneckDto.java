package com.marketinghub.experiment.service.cockpit;

/** Gargalo principal calculado pelo backend para explicar onde a venda travou. */
public record ExperimentCockpitBottleneckDto(
    String code,
    String title,
    String severity,
    String diagnosis,
    String commercialImpact,
    String recommendedFocus) {}
