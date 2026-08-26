package com.marketinghub.businessprocesschain.service.updateDraft;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Responsabilidade: transportar os campos editáveis de um rascunho de cadeia de valor. */
public record BusinessProcessChainSaveRequest(
    @NotBlank @Size(max = 160) String name,
    @NotBlank String purpose,
    @NotBlank @Size(max = 500) String outcomeDescription,
    @NotBlank @Size(max = 200) String primaryMetric,
    @NotEmpty List<@Valid BusinessProcessChainItemRequest> processes) {}
