package com.marketinghub.catalogovivo.v1.service.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: receber texto de nova versão sem sobrescrever versões anteriores. */
public record CatalogDraftRequest(
    @NotBlank @Size(max = 100000) String text,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 1000) String reason) {}
