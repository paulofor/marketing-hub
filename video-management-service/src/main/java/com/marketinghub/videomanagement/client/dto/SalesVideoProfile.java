package com.marketinghub.videomanagement.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesVideoProfile(
        Long id,
        Long productId,
        Long landingPageId,
        String videoKind,
        String title,
        String personaName,
        String personaStyle,
        String voiceStyle,
        String language,
        Integer targetDurationSeconds,
        SalesVideoStatus status,
        Instant createdAt,
        Instant updatedAt,
        SalesVideoScript latestScript,
        SalesVideoJob lastJob) {
}
