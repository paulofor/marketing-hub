package com.marketinghub.product.service.experimentcomparison;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Resumo comparativo de um experimento vinculado ao produto. */
public record ProductExperimentComparisonExperimentResponse(
    Long experimentId,
    String name,
    String status,
    String campaignStatus,
    String campaignObjective,
    String experimentType,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal dailyBudget,
    BigDecimal unitPrice,
    Long impressions,
    Long reach,
    Long clicks,
    Long leads,
    BigDecimal spend,
    BigDecimal cpc,
    BigDecimal cpl,
    Long approvedCreatives,
    Long totalCreatives,
    List<ProductExperimentComparisonFunnelStageResponse> funnelStages,
    String hypothesis,
    String promise,
    String learnedLessons,
    String recommendedAction,
    Instant updatedAt) {}
