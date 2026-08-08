package com.marketinghub.product.service.experimentcomparison;

import java.util.List;

/** Resposta consolidada para comparar experimentos de um mesmo produto. */
public record ProductExperimentComparisonResponse(
    Long productId,
    String productName,
    String productSlug,
    String commercialStatus,
    String productFormat,
    String deliveryMode,
    String revenueModel,
    String valueUnit,
    String valueEvidenceMetric,
    String validationDefinitionVersion,
    String mainRecommendation,
    List<ProductExperimentComparisonExperimentResponse> experiments) {}
