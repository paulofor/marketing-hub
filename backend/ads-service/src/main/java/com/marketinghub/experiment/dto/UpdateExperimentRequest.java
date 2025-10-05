package com.marketinghub.experiment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * Request body for updating an experiment.
 */
@Data
public class UpdateExperimentRequest {
    private String name;
    private String hypothesis;
    @JsonProperty("kpiTarget")
    private BigDecimal kpiTargetCpl;
    private String metricPresetId;
    private Integer sampleSize;
    @JsonProperty("mde")
    private BigDecimal mdePercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer creativesToGenerate;
    private Boolean creativeApproved;
    private String salesFunnelName;
    private Long facebookPageId;
    @JsonIgnore
    private boolean facebookPageIdPresent;
    private Long instagramAccountId;
    @JsonIgnore
    private boolean instagramAccountIdPresent;

    @JsonSetter(value = "facebookPageId", nulls = Nulls.SET)
    public void setFacebookPageId(Long facebookPageId) {
        this.facebookPageId = facebookPageId;
        this.facebookPageIdPresent = true;
    }

    @JsonSetter(value = "instagramAccountId", nulls = Nulls.SET)
    public void setInstagramAccountId(Long instagramAccountId) {
        this.instagramAccountId = instagramAccountId;
        this.instagramAccountIdPresent = true;
    }
}

