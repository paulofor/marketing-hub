package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da requisição para informar falha no processamento de um pacote.
 */
public record LeadPortalWorkerImageFailureRequest(@NotBlank String reason) {
}
