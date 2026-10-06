package com.marketinghub.businessprocesschain.learningcycle.v1.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Registra a primeira prova de implementação da candidata já declarada, sem trocar sua versão. */
public record RegisterCyclePrototypeRequest(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @NotBlank @Size(max = 160) String operatorName,
    @NotNull JsonNode privatePrototype) {}
