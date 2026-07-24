package com.marketinghub.salesvideo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo comercial de performance dos vídeos de um perfil.
 */
@Data
@Builder
public class SalesVideoPerformanceSummaryDto {
    private Long profileId;
    private String tenantId;
    private long totalEvents;
    private long totalLeads;
    private long totalPurchases;
    private BigDecimal totalRevenue;
    private List<SalesVideoVariantPerformanceDto> variants;
    private List<SalesVideoProviderScoreDto> providerScores;
}
