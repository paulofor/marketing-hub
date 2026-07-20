package com.marketinghub.pde.dto;

import java.util.List;

/** Retorna jornadas individuais por sessão para diagnosticar abandono no PDE. */
public record FunnelAnalyticsJourneyResponse(
        String productSlug,
        long totalSessions,
        List<FunnelAnalyticsSessionJourneyDto> sessions
) {}
