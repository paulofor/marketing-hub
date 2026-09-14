package com.marketinghub.product.executionprofile.v1.service.bind;

import jakarta.validation.constraints.*;

/** Responsabilidade: fixar uma ficha na referência exata do produto e do ciclo opcional. */
public record BindProfileRequest(
    @NotBlank @Size(max = 191) String sourceReference,
    Long learningCycleId,
    @NotBlank @Size(max = 100) String actor) {}
