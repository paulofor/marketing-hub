package com.marketinghub.catalogovivo.v1.service.adoption;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsabilidade: identificar a passagem revisada pelo operador antes de aderir ao piloto. */
public record OpalaAdoptionRequest(
    @NotNull Long expectedRevision,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 1000) String reason) {}
