package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingCandidateType;
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

    private List<String> path;

    @JsonProperty("search_locale")
    private String searchLocale;

    @JsonProperty("search_country")
    private String searchCountry;

    @JsonProperty("search_term")
    private String searchTerm;

    private Instant createdAt;

    private Instant updatedAt;
}
