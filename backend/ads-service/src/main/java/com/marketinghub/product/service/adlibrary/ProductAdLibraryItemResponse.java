package com.marketinghub.product.service.adlibrary;

import java.time.Instant;

/** Responsabilidade: representar um anúncio reutilizável encontrado no histórico do produto. */
public record ProductAdLibraryItemResponse(
    Long creativeId,
    Long experimentId,
    String experimentName,
    String experimentStatus,
    String format,
    String status,
    String headline,
    String primaryText,
    String description,
    String cta,
    String destinationUrl,
    String imageUrl,
    String videoUrl,
    String videoId,
    String reuseRecommendation,
    Instant reviewedAt) {}
