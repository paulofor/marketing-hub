package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingOptionSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TargetingOptionDto {
    private Long id;

    @JsonProperty("facebook_id")
    private String facebookId;

    private String name;

    private TargetingCandidateType type;

    @JsonProperty("audience_size")
    private Long audienceSize;

    @JsonProperty("match_score")
    private BigDecimal matchScore;

    @JsonProperty("final_score")
    private BigDecimal finalScore;

    private List<String> path;

    @JsonProperty("search_locale")
    private String searchLocale;

    @JsonProperty("search_country")
    private String searchCountry;

    @JsonProperty("search_term")
    private String searchTerm;

    @JsonProperty("source")
    private TargetingOptionSource source;

    @JsonProperty("seed_variant")
    private String seedVariant;

    private Instant createdAt;

    private Instant updatedAt;
}
