package com.marketinghub.product.dto;

import java.time.Instant;

/** Responsabilidade: expor um artigo científico associado ao mecanismo de produto. */
public record ProductScientificArticleDto(
    Long id,
    Long productId,
    String link,
    String originalTitle,
    String portugueseTitle,
    String summary,
    String mechanismApplication,
    Instant createdAt,
    Instant updatedAt) {}
