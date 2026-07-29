package com.marketinghub.pde.dto;

import java.util.List;

/** Retorna o resumo de analytics e funil comercial do produto PDE. */
public record FunnelAnalyticsSummaryResponse(
        String productSlug,
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
        long pedEntries,
        long pageViews,
        long loginStarted,
        long loginCompleted,
        long paywallViewed,
        long subscriptionClicked,
        long subscriptionApproved,
        long accessReleased,
        long firstUse,
        long checkoutStarted,
        long totalVisibleMs,
        String lastEventAt,
        List<FunnelAnalyticsEventMetricDto> events,
        List<FunnelAnalyticsExperienceVersionMetricDto> experienceVersions,
        List<FunnelAnalyticsTrafficSourceMetricDto> trafficSources,
        List<FunnelAnalyticsTrafficQualityMetricDto> trafficQualityBreakdown,
        List<FunnelAnalyticsDeviceMetricDto> deviceBreakdown,
        List<FunnelAnalyticsScreenSizeMetricDto> screenSizeBreakdown,
        List<FunnelAnalyticsSessionJourneyDto> recentJourneys
) {}
