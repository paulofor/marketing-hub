package com.marketinghub.experiment.monitoring.dto;

import java.util.List;

/** Resume uma jornada recente do PDE no painel pós-deploy administrativo. */
public record PostDeployPdeSessionJourneyDto(
    String sessionId,
    String visitorId,
    String clientIp,
    String userAgent,
    String trafficQuality,
    String trafficQualityReason,
    String trafficProvider,
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
    String lastActionName) {}
