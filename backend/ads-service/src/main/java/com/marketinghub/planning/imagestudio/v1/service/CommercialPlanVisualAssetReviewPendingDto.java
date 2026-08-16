package com.marketinghub.planning.imagestudio.v1.service;

import java.util.List;

/** Responsabilidade: entregar o entregável visual a uma execução independente de revisão. */
public record CommercialPlanVisualAssetReviewPendingDto(
    Long assetId,
    Long jobId,
    Long commercialPlanId,
    String planName,
    String offer,
    String targetAudience,
    String assetUrl,
    String label,
    List<String> purposes,
    String producerExecutionId) {}
