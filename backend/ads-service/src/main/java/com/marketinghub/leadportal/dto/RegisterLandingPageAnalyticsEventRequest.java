package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Contrato público para registrar eventos de analytics emitidos pela landing publicada.
 */
public record RegisterLandingPageAnalyticsEventRequest(
        @NotBlank String eventId,
        @NotBlank String eventType,
        String visitorId,
        String sessionId,
        String sectionId,
        Long visibleMs,
        Long elapsedMs,
        String pageUrl,
        Instant occurredAt,
        String userAgent,
        String deviceType,
        String operatingSystem,
        Integer screenWidth,
        Integer screenHeight,
        Long loadDurationMs,
        Long domContentLoadedMs,
        Long firstContentfulPaintMs,
        Integer resourceErrorCount,
        String connectionType) {

    /**
     * Mantém compatibilidade com emissores que ainda enviam somente o contrato original de analytics.
     */
    public RegisterLandingPageAnalyticsEventRequest(
            String eventId,
            String eventType,
            String visitorId,
            String sessionId,
            String sectionId,
            Long visibleMs,
            Long elapsedMs,
            String pageUrl,
            Instant occurredAt,
            String userAgent,
            String deviceType,
            String operatingSystem,
            Integer screenWidth,
            Integer screenHeight) {
        this(eventId, eventType, visitorId, sessionId, sectionId, visibleMs, elapsedMs, pageUrl, occurredAt, userAgent,
                deviceType, operatingSystem, screenWidth, screenHeight, null, null, null, null, null);
    }
}
