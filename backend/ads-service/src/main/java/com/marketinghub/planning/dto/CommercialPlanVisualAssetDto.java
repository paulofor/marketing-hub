package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor um item governado da biblioteca audiovisual do plano comercial. */
public record CommercialPlanVisualAssetDto(
    Long id,
    String assetUrl,
    String mediaType,
    String label,
    String purpose,
    List<String> purposes,
    String origin,
    String rightsStatement,
    String contentSha256,
    String creativePackageId,
    Integer versionNumber,
    CommercialPlanVisualAssetStatus status,
    Long sourceAssetId,
    CommercialPlanVisualAssetReviewStatus agentReviewStatus,
    String agentReviewSummary,
    CommercialPlanVisualAssetReviewStatus customerReviewStatus,
    String customerReviewSummary,
    Instant createdAt,
    Instant updatedAt) {}
