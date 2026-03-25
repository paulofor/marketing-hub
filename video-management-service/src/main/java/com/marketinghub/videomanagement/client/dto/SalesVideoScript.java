package com.marketinghub.videomanagement.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesVideoScript(
        Long id,
        Integer version,
        String scriptText,
        String hookText,
        String ctaText,
        String captionText,
        String storyboardJson,
        String source,
        String model,
        String prompt,
        SalesVideoScriptStatus status,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt) {
}
