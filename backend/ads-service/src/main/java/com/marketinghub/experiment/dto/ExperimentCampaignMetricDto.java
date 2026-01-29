package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ExperimentCampaignMetricDto {
    private LocalDate dateStart;
    private LocalDate dateStop;
    private Long impressions;
    private Long clicks;
    private Long leads;
    private BigDecimal spend;
    private BigDecimal cpc;
    private BigDecimal cpl;
    private Instant lastSyncedAt;
    private String lastSyncError;
}
