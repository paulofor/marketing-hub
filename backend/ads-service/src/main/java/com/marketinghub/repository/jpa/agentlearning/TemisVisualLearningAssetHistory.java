package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import java.math.BigDecimal;

/** Responsabilidade: transportar o histórico visual leve de um entregável revisado por Têmis. */
public record TemisVisualLearningAssetHistory(
    Long assetId,
    Long commercialPlanId,
    String label,
    Integer versionNumber,
    CommercialPlanVisualAssetStatus status,
    CommercialPlanVisualAssetReviewStatus reviewStatus,
    String reviewerExecutionId,
    String reviewJson,
    String reviewRequestJson,
    String reviewResponseJson,
    Long jobId,
    String purposesJson,
    String size,
    String playbookVersion,
    String playbookContextKey,
    BigDecimal costUsd) {}
