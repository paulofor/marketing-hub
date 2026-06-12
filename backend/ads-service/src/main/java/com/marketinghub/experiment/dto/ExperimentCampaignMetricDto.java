package com.marketinghub.experiment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

/**
 * Transporta as métricas agregadas da campanha Facebook para relatórios e telas do experimento.
 */
@Data
public class ExperimentCampaignMetricDto {
    private LocalDate dateStart;
    private LocalDate dateStop;
    private Long reach;
    private Long impressions;
    private Long clicks;
    private Long leads;
    private BigDecimal spend;
    private BigDecimal cpc;
    private BigDecimal cpl;
    private Instant lastSyncedAt;
    private String lastSyncError;
}
