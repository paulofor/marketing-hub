package com.marketinghub.product.dto;

import java.time.Instant;

/** Responsabilidade: expor personagem/avatar de vídeo cadastrado para uso em próximos renders. */
public record ProductVideoProviderAvatarDto(
    Long id,
    Long productId,
    Long sourceAssetId,
    String provider,
    String characterName,
    String providerAvatarId,
    String providerAvatarGroupId,
    String providerStatus,
    String sourceImageUrl,
    boolean supportsReusableAvatar,
    String notes,
    Instant createdAt,
    Instant updatedAt) {}
