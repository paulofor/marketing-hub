package com.marketinghub.businessprocess.automation.v1.service.status;

/**
 * Responsabilidade: transportar a identidade sem carregar estado mutável antes do lock do produto.
 */
public record ProcessRunIdentity(Long productId, Long processDefinitionId) {}
