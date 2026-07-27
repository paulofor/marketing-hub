package com.marketinghub.salesvideo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Pontuação comercial consolidada de um provedor de vídeo.
 */
@Data
@Builder
public class SalesVideoProviderScoreDto {
    private String providerName;
    private int score;
    private long readyJobs;
    private long failedJobs;
    private long operationalFailedJobs;
    private long approvedAssets;
    private long rejectedAssets;
    private long leads;
    private long qualifiedLeads;
    private long checkoutStarts;
    private long purchases;
    private BigDecimal revenue;
    private String recommendation;
    private String riskCategory;
    private String riskMessage;
}
