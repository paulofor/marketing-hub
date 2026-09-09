package com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Responsabilidade: transportar uma retentativa idempotente da conciliação automática. */
public record ReconcileLearningCycleMeasurementRequest(
    @NotNull UUID requestKey, @NotNull @Min(0) Long expectedRevision) {}
