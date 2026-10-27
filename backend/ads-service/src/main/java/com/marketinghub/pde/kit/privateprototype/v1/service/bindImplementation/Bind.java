package com.marketinghub.pde.kit.privateprototype.v1.service.bindImplementation;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Responsabilidade: registrar escolha explícita da capacidade para o mesmo desenho aprovado. */
public record Bind(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @NotNull @Min(1) Long productId,
    @NotNull @Min(1) Long cycleId,
    @NotNull @Min(1) Long architectureTaskId,
    @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String architectureSha256,
    @NotBlank String profileCode,
    @NotBlank @Size(max = 160) String operatorName) {}
