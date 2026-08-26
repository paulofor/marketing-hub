package com.marketinghub.businessprocesschain.service.updateDraft;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsabilidade: declarar um processo publicado e sua contribuição na cadeia editável. */
public record BusinessProcessChainItemRequest(
    @NotNull Long processDefinitionId,
    @NotBlank @Size(max = 500) String valueContribution) {}
