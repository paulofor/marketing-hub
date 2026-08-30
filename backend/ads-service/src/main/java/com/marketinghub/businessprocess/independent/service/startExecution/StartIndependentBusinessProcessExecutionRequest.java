package com.marketinghub.businessprocess.independent.service.startExecution;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Contrato idempotente para iniciar um processo sem associação artificial a produto. */
public record StartIndependentBusinessProcessExecutionRequest(
    @NotNull UUID requestKey,
    @NotNull Long processDefinitionId,
    @NotBlank @Size(max = 100) String requestedByName,
    @NotNull JsonNode input) {}
