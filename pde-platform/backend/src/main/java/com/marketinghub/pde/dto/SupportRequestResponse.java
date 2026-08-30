package com.marketinghub.pde.dto;

/** Confirma o registro de suporte sem reexpor o conteúdo de uma workspace paga. */
public record SupportRequestResponse(
        String supportStatus,
        String message,
        String requestedAt
) {}
