package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TargetingCandidateResolutionUpdateRequest {
    private TargetingCandidateStatus status;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    private List<OptionPayload> options = new ArrayList<>();

    @Data
    public static class OptionPayload {
        @JsonProperty("facebook_id")
        private String facebookId;

        private String name;

        private TargetingCandidateType type;

        @JsonProperty("audience_size")
        private Long audienceSize;

        @JsonProperty("match_score")
        private BigDecimal matchScore;

        private List<String> path = new ArrayList<>();

        @JsonProperty("search_locale")
        private String searchLocale;

        @JsonProperty("search_country")
        private String searchCountry;

        @JsonProperty("search_term")
        private String searchTerm;
    }
}
