package com.marketinghub.catalogovivo.v1.service.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: registrar revisão explícita do texto e dos limites da atividade. */
public record CatalogReviewRequest(
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 1000) String reason,
    @NotBlank String expectedSha256) {}
