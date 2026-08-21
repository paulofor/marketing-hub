package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Recebe uma solicitação curta de suporte ou revisão feita dentro da área da cliente. */
public record SupportRequest(
        @NotBlank @Size(max = 2000) String message
) {}
