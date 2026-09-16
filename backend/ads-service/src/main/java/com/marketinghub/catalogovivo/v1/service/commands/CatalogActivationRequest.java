package com.marketinghub.catalogovivo.v1.service.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Responsabilidade: trocar o conjunto completo em transação com proteção contra tela desatualizada.
 */
public record CatalogActivationRequest(
    @NotEmpty Map<Long, Long> versions,
    @NotEmpty Map<Long, Long> expectedActiveVersions,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 1000) String reason) {}
