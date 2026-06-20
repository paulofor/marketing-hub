package com.marketinghub.experiment.service.generatepromise;

import java.util.List;

/** Responsabilidade: transportar o estado da solicitação, o prompt do worker e as opções de promessa geradas. */
public record GenerateExperimentPromiseOptionsResponse(Long requestId, String status, String prompt, List<ExperimentPromiseOptionDto> options) {}
