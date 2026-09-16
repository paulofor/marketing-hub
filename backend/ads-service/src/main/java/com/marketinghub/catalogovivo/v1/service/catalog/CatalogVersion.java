package com.marketinghub.catalogovivo.v1.service.catalog;

import java.time.Instant;

/** Responsabilidade: apresentar uma versão textual e sua revisão sem permitir edição destrutiva. */
public record CatalogVersion(
    long id,
    long bindingId,
    int versionNumber,
    String text,
    String sha256,
    String status,
    String createdBy,
    Instant createdAt,
    String reviewedBy,
    Instant reviewedAt,
    String reviewNote) {}
