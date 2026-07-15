package com.marketinghub.experiment.service.construction;

/** Etapa resumida do fluxo manual com status validado pelo backend. */
public record ExperimentConstructionStepDto(
        String code,
        String title,
        String description,
        String tab,
        String action,
        boolean validated,
        String validationLabel) {}
