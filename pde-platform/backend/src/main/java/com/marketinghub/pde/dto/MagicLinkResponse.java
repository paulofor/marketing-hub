package com.marketinghub.pde.dto;

/** Retorna o estado do envio do link magico da Área MUSA. */
public record MagicLinkResponse(
        String productSlug,
        String email,
        String deliveryStatus,
        String accessUrl
) {}
