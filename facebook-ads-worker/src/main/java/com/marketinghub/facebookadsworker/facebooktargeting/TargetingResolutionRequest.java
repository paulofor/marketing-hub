package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload enviado pelo backend contendo os candidatos a serem validados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetingResolutionRequest {
    @JsonProperty("ad_account_id")
    private String adAccountId;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("country")
    private String country;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("candidates")
    private List<TargetingCandidatePayload> candidates = new ArrayList<>();

    public String getAdAccountId() {
        return adAccountId;
    }

    public void setAdAccountId(String adAccountId) {
        this.adAccountId = adAccountId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public List<TargetingCandidatePayload> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<TargetingCandidatePayload> candidates) {
        this.candidates = candidates;
    }
}
