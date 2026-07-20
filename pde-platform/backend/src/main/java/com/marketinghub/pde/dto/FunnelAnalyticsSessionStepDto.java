package com.marketinghub.pde.dto;

/** Descreve um evento ordenado dentro da jornada de uma sessão PDE. */
public record FunnelAnalyticsSessionStepDto(
        String occurredAt,
        String eventType,
        String screenName,
        String sectionId,
        String actionName,
        Long visibleMs,
        Long scrollDepthPercent,
        String fieldName,
        String elementText,
        String pageUrl
) {}
