package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisição utilizado para confirmar o resultado do envio do pacote pelo serviço de e-mail.
 */
public record LeadPortalImagePackageAckRequest(
        @NotNull Boolean success,
        String errorMessage
) {
}
