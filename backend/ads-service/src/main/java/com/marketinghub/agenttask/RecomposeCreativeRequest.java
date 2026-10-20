package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: registrar autoria e motivo da recomposição sem nova inferência. */
public record RecomposeCreativeRequest(
    @NotBlank @Size(max = 160) String requestedByName, @NotBlank @Size(max = 2000) String reason) {}
