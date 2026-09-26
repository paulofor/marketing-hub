package com.marketinghub.experiment.service.createFacebookSuccessor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Recebe a origem comercial auditada que um sucessor Facebook planejado deve adotar. */
public record AdoptFacebookSuccessorRequest(@NotNull @Positive Long sourceExperimentId) {}
