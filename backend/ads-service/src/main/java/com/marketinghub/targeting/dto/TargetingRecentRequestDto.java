package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TargetingRecentRequestDto {
    private UUID id;

    @JsonProperty("descricao")
    private String descricao;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("seed_keywords")
    private List<String> seedKeywords;

    @JsonProperty("meta_ads_keywords")
    private List<String> metaAdsKeywords;
}
