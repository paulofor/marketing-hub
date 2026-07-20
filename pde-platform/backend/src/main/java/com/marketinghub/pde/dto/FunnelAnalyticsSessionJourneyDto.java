package com.marketinghub.pde.dto;

import java.util.List;

/** Resume uma sessão individual do funil para revelar avanço e ponto de abandono. */
public record FunnelAnalyticsSessionJourneyDto(
        String sessionId,
        String visitorId,
        String firstEventAt,
        String lastEventAt,
        long totalVisibleMs,
        long maxScrollDepthPercent,
        List<String> screenNames,
        List<String> sectionIds,
        boolean fieldFocused,
        boolean fieldInputStarted,
        boolean fieldFilled,
        boolean ctaClicked,
        boolean loginStarted,
        boolean loginCompleted,
        boolean paywallViewed,
        boolean checkoutStarted,
        boolean subscriptionApproved,
        String abandonmentPoint,
        String lastEventType,
        String lastActionName,
        List<FunnelAnalyticsSessionStepDto> steps
) {}
