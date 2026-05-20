package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record RegisterLandingPageAnalyticsEventRequest(
        @NotBlank String eventId,
        @NotBlank String eventType,
        String sessionId,
        String sectionId,
        Long visibleMs,
        Long elapsedMs,
        String pageUrl,
        Instant occurredAt,
        String userAgent) {
}
