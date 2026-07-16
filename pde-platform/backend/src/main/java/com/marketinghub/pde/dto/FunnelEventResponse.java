package com.marketinghub.pde.dto;

/** Retorna a confirmação de registro de evento comercial PED/MUSA. */
public record FunnelEventResponse(
        String eventId,
        String eventType,
        String status
) {}
