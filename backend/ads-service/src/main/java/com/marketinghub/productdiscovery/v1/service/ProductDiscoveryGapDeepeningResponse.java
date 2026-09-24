package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resume a política de evidências e os limites da atividade de aprofundamento de lacunas. */
public record ProductDiscoveryGapDeepeningResponse(
    Long cycleId,
    boolean applicable,
    ProductDiscoveryCycleStatus cycleStatus,
    String stageCode,
    int minimumInterviews,
    int maximumInterviews,
    int interviewCount,
    int purchasedCount,
    int abandonedCount,
    List<Long> coveredOpportunityIds,
    List<Long> missingOpportunityIds,
    boolean readyForResearch,
    int maximumPublicQueriesPerAttempt,
    int maximumAttempts,
    int maximumModelInvocations,
    BigDecimal maximumSearchCostUsd,
    String searchCostCoverage,
    String modelCostCoverage,
    String searchPricingSource,
    LocalDate searchPricingObservedOn,
    String guidance,
    List<ProductDiscoveryCustomerInterviewResponse> interviews,
    String evidencePolicy,
    boolean canAdoptPublicEvidence,
    boolean canResumePublicResearch) {}
