package com.marketinghub.experiment.service.createFacebookSuccessor;

import java.util.List;

/** Expõe a decisão canônica sobre criar ou abrir o sucessor Facebook de um experimento. */
public record FacebookSuccessorReadinessResponse(
    boolean available, Long existingSuccessorId, List<String> blockers) {}
