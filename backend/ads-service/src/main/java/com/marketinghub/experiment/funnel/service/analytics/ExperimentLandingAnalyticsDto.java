package com.marketinghub.experiment.funnel.service.analytics;

import java.time.Instant;
import java.util.List;

/** Consolida as métricas de analytics da landing vinculada ao experimento. */
public record ExperimentLandingAnalyticsDto(
    long totalEvents,
    long totalSessions,
    long pageViews,
    long sectionViewEvents,
    long totalVisibleMs,
    long averageVisibleMsPerSession,
    Instant lastEventAt,
    List<ExperimentLandingAnalyticsDeviceDto> deviceBreakdown,
    List<ExperimentLandingAnalyticsOperatingSystemDto> mobileOperatingSystemBreakdown,
    List<ExperimentLandingAnalyticsScreenSizeDto> screenSizeBreakdown,
    ExperimentLandingAnalyticsLoadMetricDto loadMetrics,
    ExperimentLandingAnalyticsVisitorsDto visitors,
    ExperimentLandingAnalyticsTrafficQualityDto trafficQuality,
    List<ExperimentLandingAnalyticsSessionDto> sessions) {

  /** Mantém compatibilidade com consumidores internos anteriores à classificação de tráfego. */
  public ExperimentLandingAnalyticsDto(
      long totalEvents,
      long totalSessions,
      long pageViews,
      long sectionViewEvents,
      long totalVisibleMs,
      long averageVisibleMsPerSession,
      Instant lastEventAt,
      List<ExperimentLandingAnalyticsDeviceDto> deviceBreakdown,
      List<ExperimentLandingAnalyticsOperatingSystemDto> mobileOperatingSystemBreakdown,
      List<ExperimentLandingAnalyticsScreenSizeDto> screenSizeBreakdown,
      ExperimentLandingAnalyticsLoadMetricDto loadMetrics,
      ExperimentLandingAnalyticsVisitorsDto visitors,
      List<ExperimentLandingAnalyticsSessionDto> sessions) {
    this(
        totalEvents,
        totalSessions,
        pageViews,
        sectionViewEvents,
        totalVisibleMs,
        averageVisibleMsPerSession,
        lastEventAt,
        deviceBreakdown,
        mobileOperatingSystemBreakdown,
        screenSizeBreakdown,
        loadMetrics,
        visitors,
        new ExperimentLandingAnalyticsTrafficQualityDto(totalSessions, 0, 0),
        sessions);
  }
}
