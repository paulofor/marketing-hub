package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Resume analytics do PDE usados para medir interação e intenção comercial. */
public record PostDeployPdeSummaryDto(
    boolean available,
    String status,
    String errorMessage,
    String measurementMode,
    String measurementLabel,
    String measurementRecommendation,
    String currentExperienceVersion,
    long totalEvents,
    long rawTotalEvents,
    long uniqueVisitors,
    long sessions,
    long rawSessions,
    long humanSessions,
    long botSuspectedSessions,
    long platformCrawlerSessions,
    long internalQaSessions,
    long unknownSessions,
    long pdeEntries,
    long pageViews,
    long presenceMapClicks,
    long diagnosticClicks,
    long fieldFilled,
    long loginStarted,
    long loginCompleted,
    long paywallViewed,
    long subscriptionClicked,
    long checkoutStarted,
    long subscriptionApproved,
    long totalVisibleMs,
    long averageVisibleMsPerSession,
    Instant lastEventAt,
    Map<String, Long> events,
    List<PostDeployPdeExperienceVersionDto> experienceVersions,
    List<PostDeployPdeTrafficSourceDto> trafficSources,
    List<PostDeployPdeTrafficQualityDto> trafficQualityBreakdown,
    List<PostDeployPdeDeviceDto> deviceBreakdown,
    List<PostDeployPdeScreenSizeDto> screenSizeBreakdown,
    List<PostDeployPdeSessionJourneyDto> recentJourneys) {}
