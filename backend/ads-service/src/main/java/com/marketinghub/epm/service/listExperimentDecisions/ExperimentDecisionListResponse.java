package com.marketinghub.epm.service.listExperimentDecisions;

import java.util.List;

/** Resposta com as decisões financeiras de um experimento. */
public record ExperimentDecisionListResponse(List<ExperimentDecisionResponse> decisions) {}
