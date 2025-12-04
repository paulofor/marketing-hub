package com.marketinghub.leadportal.dto;

/**
 * Corpo da requisição utilizado para solicitar um novo processamento de pacote.
 */
public record LeadPortalWorkerImageRetryRequest(String reason) {
}
