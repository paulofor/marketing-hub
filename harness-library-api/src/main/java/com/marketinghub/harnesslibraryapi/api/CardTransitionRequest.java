package com.marketinghub.harnesslibraryapi.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Recebe a justificativa obrigatória para uma mudança de estado editorial. */
public record CardTransitionRequest(@NotBlank @Size(max = 500) String reason) {}
