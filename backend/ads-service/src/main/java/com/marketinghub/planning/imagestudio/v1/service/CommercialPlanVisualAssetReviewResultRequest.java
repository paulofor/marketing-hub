package com.marketinghub.planning.imagestudio.v1.service;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;

/** Responsabilidade: receber o parecer independente de Têmis sobre um entregável gerado. */
public record CommercialPlanVisualAssetReviewResultRequest(
    CommercialPlanVisualAssetReviewStatus decision,
    String reviewerExecutionId,
    String summary,
    String requestJson,
    String responseJson,
    String error) {}
