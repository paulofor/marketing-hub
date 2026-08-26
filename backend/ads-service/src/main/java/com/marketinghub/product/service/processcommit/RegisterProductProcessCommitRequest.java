package com.marketinghub.product.service.processcommit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contrato para registrar um commit realizado para um produto durante um processo conhecido. */
public record RegisterProductProcessCommitRequest(
    @NotNull Long processDefinitionId,
    @NotBlank @Size(max = 160) String repositoryName,
    @NotBlank
        @Pattern(
            regexp = "(?i)^[0-9a-f]{40}([0-9a-f]{24})?$",
            message = "Informe o SHA completo do commit.")
        String commitSha,
    @NotBlank @Size(max = 500) String commitSummary,
    @Size(max = 512) @Pattern(regexp = "^https://.+", message = "A URL do commit deve usar HTTPS.")
        String commitUrl,
    @NotBlank @Size(max = 191) String recordedBy) {}
