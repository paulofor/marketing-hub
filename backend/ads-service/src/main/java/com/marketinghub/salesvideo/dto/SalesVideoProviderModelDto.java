package com.marketinghub.salesvideo.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de leitura do catálogo e de seus gates de homologação. */
public record SalesVideoProviderModelDto(
    Long id,
    String code,
    String displayName,
    String manufacturerName,
    String aggregatorName,
    String providerAccountKey,
    String routeKey,
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
    String notes,
    BigDecimal pricingAmountUsd,
    String pricingUnit,
    BigDecimal pricingQuantity,
    String pricingResolution,
    Boolean pricingIncludesAudio,
    String pricingSourceUrl,
    Instant pricingObservedAt,
    String pricingResearchStatus,
    String pricingResearchNotes,
    BigDecimal normalizedCostPerSecondUsd,
    boolean pricingStale) {}
