package com.marketinghub.emailservice.service.client;

/**
 * Corpo enviado ao backend para confirmar o resultado do envio do e-mail.
 */
public record LeadPortalImagePackageAckRequest(
        boolean success,
        String errorMessage
) {
}
