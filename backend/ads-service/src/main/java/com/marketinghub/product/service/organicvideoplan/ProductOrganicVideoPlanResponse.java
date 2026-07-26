package com.marketinghub.product.service.organicvideoplan;

import java.util.List;

/** Responsabilidade: transportar o playbook de vídeos orgânicos recomendado para um produto. */
public record ProductOrganicVideoPlanResponse(
    Long productId,
    String productName,
    String productSlug,
    String strategyName,
    String objective,
    String publishingWindow,
    String channelPriority,
    String mixRationale,
    List<ProductOrganicVideoPlanItemResponse> videos,
    List<ProductOrganicVideoDecisionRuleResponse> decisionRules,
    List<String> operatingPrinciples) {}
