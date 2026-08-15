package com.marketinghub.businessprocess;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

/** Contrato de cadastro de uma versão de processo com diagrama estruturado. */
public record BusinessProcessDefinitionRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 100) String processCode,
    @NotBlank @Size(max = 160) String name,
    @NotBlank @Size(max = 5000) String purpose,
    @NotBlank @Size(max = 120) String ownerName,
    @NotBlank @Size(max = 500) String triggerDescription,
    @NotBlank @Size(max = 500) String outcomeDescription,
    @NotNull @Min(1) Integer versionNumber,
    @Size(max = 200) String technicalReference,
    @NotNull JsonNode diagram) {}
