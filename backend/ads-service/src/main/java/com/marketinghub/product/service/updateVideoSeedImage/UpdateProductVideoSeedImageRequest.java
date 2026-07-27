package com.marketinghub.product.service.updateVideoSeedImage;

import com.marketinghub.product.ProductVideoSeedImageReviewStatus;

/** Responsabilidade: transportar a decisão humana sobre a imagem semente de vídeo do produto. */
public record UpdateProductVideoSeedImageRequest(
    Long assetId,
    String characterName,
    ProductVideoSeedImageReviewStatus reviewStatus,
    String reviewNotes,
    String reviewedBy) {}
