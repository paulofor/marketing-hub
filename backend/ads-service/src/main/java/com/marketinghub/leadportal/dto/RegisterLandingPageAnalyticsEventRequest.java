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
        String deviceType) {
}
