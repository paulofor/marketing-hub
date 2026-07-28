package com.marketinghub.experiment.monitoring.pde;

import java.util.List;

/** Contrato mínimo do resumo de analytics retornado pelo backend PDE. */
public record PdeAnalyticsSummary(
    String productSlug,
    String currentExperienceVersion,
    long totalEvents,
    long uniqueVisitors,
    long sessions,
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
    List<PdeEventMetric> events,
    List<PdeExperienceVersionMetric> experienceVersions,
    List<PdeTrafficSourceMetric> trafficSources,
    List<PdeDeviceMetric> deviceBreakdown,
    List<PdeScreenSizeMetric> screenSizeBreakdown,
    List<PdeSessionJourney> recentJourneys) {
  /** Representa a contagem agregada por tipo de evento do PDE. */
  public record PdeEventMetric(String eventType, long total) {}

  /** Representa a contagem comercial agregada por versão da experiência PDE. */
  public record PdeExperienceVersionMetric(
      String experienceVersion,
      long totalEvents,
      long sessions,
      long pdeEntries,
      long presenceMapClicks,
      long diagnosticClicks,
      long videoPartial,
      long videoComplete,
      long loginStarted,
      long paywallViewed,
      long subscriptionClicked,
      long checkoutStarted,
      long subscriptionApproved) {}

  /** Representa desempenho por origem, campanha e criativo identificado por UTM. */
  public record PdeTrafficSourceMetric(
      String trafficChannel,
      String utmSource,
      String utmMedium,
      String utmCampaign,
      String utmContent,
      long sessions,
      long pdeEntries,
      long firstInteractionClicks,
      long videoPartial,
      long videoComplete,
      long loginStarted,
      long paywallViewed,
      long checkoutStarted,
      long subscriptionApproved,
      double firstInteractionRate,
      double paywallRate,
      double checkoutRate,
      double purchaseRate,
      long totalVisibleMs,
      String lastEventAt) {}

  /** Representa sessões do PDE por dispositivo capturado no navegador. */
  public record PdeDeviceMetric(
      String deviceType, String label, long sessions, double percentage) {}

  /** Representa sessões do PDE por resolução de tela capturada no navegador. */
  public record PdeScreenSizeMetric(
      String screenSize,
      String label,
      Integer width,
      Integer height,
      long sessions,
      double percentage) {}

  /** Representa uma jornada recente por sessão retornada pelo PDE. */
  public record PdeSessionJourney(
      String sessionId,
      String visitorId,
      String clientIp,
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
}
