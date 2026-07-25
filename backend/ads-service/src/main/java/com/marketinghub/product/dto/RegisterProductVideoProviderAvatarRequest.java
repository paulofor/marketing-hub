package com.marketinghub.product.dto;

/** Responsabilidade: transportar dados de cadastro de personagem/avatar de vídeo por provider. */
public record RegisterProductVideoProviderAvatarRequest(
    Long sourceAssetId,
    String provider,
    String characterName,
    String providerAvatarId,
    String providerAvatarGroupId,
    String providerStatus,
    String sourceImageUrl,
    Boolean supportsReusableAvatar,
    String notes) {}
