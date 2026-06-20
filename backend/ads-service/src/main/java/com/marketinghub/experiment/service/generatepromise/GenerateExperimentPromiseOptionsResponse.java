package com.marketinghub.experiment.service.generatepromise;

import java.util.List;

/** Responsabilidade: transportar as opções de contrato de promessa única sugeridas pela IA. */
public record GenerateExperimentPromiseOptionsResponse(List<ExperimentPromiseOptionDto> options) {}
