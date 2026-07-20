package com.marketinghub.pde.dto;

/** Retorna o resultado da limpeza de analytics antes do tráfego pago real. */
public record FunnelAnalyticsResetResponse(
        String productSlug,
        long deletedEvents,
        String status) {}
