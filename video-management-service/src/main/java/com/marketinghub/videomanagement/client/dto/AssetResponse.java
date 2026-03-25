package com.marketinghub.videomanagement.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetResponse(
        Long id,
        AssetType type,
        String url,
        String externalId,
        String payload,
        Instant createdAt,
        Instant updatedAt) {
}
