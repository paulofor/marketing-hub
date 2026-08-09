package com.marketinghub.salesvideo.dto;

/** Contrato de leitura do catálogo e de seus gates de homologação. */
public record SalesVideoProviderModelDto(
    Long id,
    String code,
    String displayName,
    String providerName,
    String providerFamily,
    String adapterKey,
    String externalModelId,
    String recommendedUse,
    String lifecycleStatus,
    Integer clipDurationSeconds,
    Integer maxDirectDurationSeconds,
    boolean supportsHeroVideo,
    boolean supportsSceneAssembly,
    boolean requiresSourceImage,
    String creditsUrl,
    String documentationUrl,
    boolean adapterVerified,
    boolean pricingVerified,
    boolean commercialLicenseVerified,
    boolean qualityGateVerified,
    String notes) {}
