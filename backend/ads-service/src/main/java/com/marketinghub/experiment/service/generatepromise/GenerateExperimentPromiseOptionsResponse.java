package com.marketinghub.experiment.service.generatepromise;

import java.util.List;

/** Responsabilidade: transportar o estado da solicitação e as opções de promessa quando já geradas. */
public record GenerateExperimentPromiseOptionsResponse(Long requestId, String status, List<ExperimentPromiseOptionDto> options) {}
