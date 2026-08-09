package com.marketinghub.salesvideo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Calcula o custo de produção de vídeos gerados por providers de IA. */
@Component
public class SalesVideoProductionCostCalculator {
  private static final BigDecimal LUMA_RAY_32_360_5S = new BigDecimal("0.0600");
  private static final BigDecimal LUMA_RAY_32_360_10S = new BigDecimal("0.1800");
  private static final BigDecimal LUMA_RAY_32_540_5S = new BigDecimal("0.1500");
  private static final BigDecimal LUMA_RAY_32_540_10S = new BigDecimal("0.4500");
  private static final BigDecimal LUMA_RAY_32_720_5S = new BigDecimal("0.3000");
  private static final BigDecimal LUMA_RAY_32_720_10S = new BigDecimal("0.9000");
  private static final BigDecimal LUMA_RAY_32_1080_5S = new BigDecimal("1.2000");
  private static final BigDecimal LUMA_RAY_32_1080_10S = new BigDecimal("3.6000");
  private static final BigDecimal KLING_30_STANDARD_720 = new BigDecimal("0.1120");
  private static final BigDecimal KLING_30_PRO_1080 = new BigDecimal("0.1400");
  private static final BigDecimal RUNWAY_GEN45 = new BigDecimal("0.1200");
  private static final BigDecimal RUNWAY_GEN4_TURBO = new BigDecimal("0.0500");
  private static final BigDecimal RUNWAY_SEEDANCE_25_480 = new BigDecimal("0.2000");
  private static final BigDecimal RUNWAY_SEEDANCE_25_720 = new BigDecimal("0.3000");
  private static final BigDecimal RUNWAY_HAILUO_3_768 = new BigDecimal("0.1000");
  private static final BigDecimal RUNWAY_HAILUO_3_2K = new BigDecimal("0.1500");
  private static final BigDecimal HEYGEN_AVATAR_IV_PHOTO_PER_SECOND = new BigDecimal("0.0500");
  private static final BigDecimal VEO_31_STANDARD_720_1080 = new BigDecimal("0.40");
  private static final BigDecimal VEO_31_STANDARD_4K = new BigDecimal("0.60");
  private static final BigDecimal VEO_31_FAST_720 = new BigDecimal("0.10");
  private static final BigDecimal VEO_31_FAST_1080 = new BigDecimal("0.12");
  private static final BigDecimal VEO_31_FAST_4K = new BigDecimal("0.30");
  private static final BigDecimal VEO_31_LITE_720 = new BigDecimal("0.05");
  private static final BigDecimal VEO_31_LITE_1080 = new BigDecimal("0.08");
  private static final BigDecimal VEO_3_STANDARD = new BigDecimal("0.40");
  private static final BigDecimal VEO_2 = new BigDecimal("0.35");
  private static final BigDecimal GEMINI_OMNI_FLASH = new BigDecimal("0.10");

  /** Estima o custo em USD com base no modelo, duração e resolução do vídeo. */
  public BigDecimal estimateUsd(
      String providerName, String model, Integer durationSeconds, String resolution) {
    Integer normalizedDuration = normalizeDuration(durationSeconds);
    if (normalizedDuration == null) {
      return null;
    }
    if (isLuma(providerName, model)) {
      return estimateLumaRay32Usd(normalizedDuration, resolution);
    }
    BigDecimal pricePerSecond = pricePerSecondUsd(providerName, model, resolution);
    if (pricePerSecond == null) {
      return null;
    }
    return pricePerSecond
        .multiply(BigDecimal.valueOf(normalizedDuration.longValue()))
        .setScale(4, RoundingMode.HALF_UP);
  }

  /** Resolve o preço oficial por segundo em USD para o modelo de vídeo conhecido. */
  public BigDecimal pricePerSecondUsd(String providerName, String model, String resolution) {
    String normalizedModel = normalize(model);
    String normalizedProvider = normalize(providerName);
    if (isKling(normalizedProvider, normalizedModel)) {
      return is1080p(resolution) ? KLING_30_PRO_1080 : KLING_30_STANDARD_720;
    }
    if (isSeedance2(normalizedProvider, normalizedModel)) {
      return is480p(resolution) ? RUNWAY_SEEDANCE_25_480 : RUNWAY_SEEDANCE_25_720;
    }
    if (isHailuo3(normalizedProvider, normalizedModel)) {
      return is2k(resolution) ? RUNWAY_HAILUO_3_2K : RUNWAY_HAILUO_3_768;
    }
    if (contains(normalizedProvider, "runway-veo-3-1-fast")) {
      return new BigDecimal("0.1500");
    }
    if (contains(normalizedProvider, "runway-veo-3-1")) {
      return VEO_31_STANDARD_720_1080;
    }
    if (contains(normalizedProvider, "runway-gen-4-turbo")) {
      return RUNWAY_GEN4_TURBO;
    }
    if (isRunway(normalizedProvider, normalizedModel)) {
      return RUNWAY_GEN45;
    }
    if (isHeyGen(normalizedProvider, normalizedModel)) {
      return HEYGEN_AVATAR_IV_PHOTO_PER_SECOND;
    }
    if (contains(normalizedModel, "omni-flash") || contains(normalizedProvider, "omni-flash")) {
      return GEMINI_OMNI_FLASH;
    }
    if (contains(normalizedModel, "veo-2")) {
      return VEO_2;
    }
    if (contains(normalizedModel, "veo-3.0") || contains(normalizedModel, "veo-3-0")) {
      return contains(normalizedModel, "fast") ? veoFastPrice(resolution) : VEO_3_STANDARD;
    }
    if (contains(normalizedModel, "lite") || contains(normalizedProvider, "lite")) {
      return veoLitePrice(resolution);
    }
    if (contains(normalizedModel, "fast") || contains(normalizedProvider, "fast")) {
      return veoFastPrice(resolution);
    }
    if (contains(normalizedModel, "veo")
        || contains(normalizedProvider, "veo")
        || contains(normalizedProvider, "real")) {
      return is4k(resolution) ? VEO_31_STANDARD_4K : VEO_31_STANDARD_720_1080;
    }
    return null;
  }

  /** Estima custo da Luma Ray 3.2 por blocos oficiais de 5s ou 10s. */
  private BigDecimal estimateLumaRay32Usd(Integer durationSeconds, String resolution) {
    BigDecimal blockPrice = lumaRay32TenSecondPrice(resolution);
    int blocks = (int) Math.ceil(durationSeconds / 10.0);
    if (durationSeconds <= 5) {
      blockPrice = lumaRay32FiveSecondPrice(resolution);
      blocks = 1;
    }
    return blockPrice.multiply(BigDecimal.valueOf(blocks)).setScale(4, RoundingMode.HALF_UP);
  }

  /** Resolve preço Luma Ray 3.2 de 5 segundos conforme resolução. */
  private BigDecimal lumaRay32FiveSecondPrice(String resolution) {
    if (is1080p(resolution)) {
      return LUMA_RAY_32_1080_5S;
    }
    if (is540p(resolution)) {
      return LUMA_RAY_32_540_5S;
    }
    if (is360p(resolution)) {
      return LUMA_RAY_32_360_5S;
    }
    return LUMA_RAY_32_720_5S;
  }

  /** Resolve preço Luma Ray 3.2 de 10 segundos conforme resolução. */
  private BigDecimal lumaRay32TenSecondPrice(String resolution) {
    if (is1080p(resolution)) {
      return LUMA_RAY_32_1080_10S;
    }
    if (is540p(resolution)) {
      return LUMA_RAY_32_540_10S;
    }
    if (is360p(resolution)) {
      return LUMA_RAY_32_360_10S;
    }
    return LUMA_RAY_32_720_10S;
  }

  /** Normaliza duração inválida para impedir custo fictício. */
  private Integer normalizeDuration(Integer durationSeconds) {
    if (durationSeconds == null || durationSeconds <= 0) {
      return null;
    }
    return durationSeconds;
  }

  /** Resolve preço do Veo Fast conforme resolução oficial. */
  private BigDecimal veoFastPrice(String resolution) {
    if (is4k(resolution)) {
      return VEO_31_FAST_4K;
    }
    if (is1080p(resolution)) {
      return VEO_31_FAST_1080;
    }
    return VEO_31_FAST_720;
  }

  /** Resolve preço do Veo Lite conforme resolução oficial. */
  private BigDecimal veoLitePrice(String resolution) {
    if (is1080p(resolution) || is4k(resolution)) {
      return VEO_31_LITE_1080;
    }
    return VEO_31_LITE_720;
  }

  /** Verifica se a resolução solicitada é 1080p. */
  private boolean is1080p(String resolution) {
    return normalize(resolution).contains("1080");
  }

  /** Verifica se a resolução solicitada é 540p. */
  private boolean is540p(String resolution) {
    return normalize(resolution).contains("540");
  }

  /** Verifica se a resolução solicitada é 360p. */
  private boolean is360p(String resolution) {
    return normalize(resolution).contains("360");
  }

  /** Verifica se a resolução solicitada é 480p. */
  private boolean is480p(String resolution) {
    return normalize(resolution).contains("480");
  }

  /** Verifica se a resolução solicitada é 4k. */
  private boolean is4k(String resolution) {
    String normalized = normalize(resolution);
    return normalized.contains("4k") || normalized.contains("2160");
  }

  /** Verifica se a resolução solicitada é 2K. */
  private boolean is2k(String resolution) {
    String normalized = normalize(resolution);
    return normalized.contains("2k") || normalized.contains("1440");
  }

  /** Normaliza texto para comparação tolerante. */
  private String normalize(String value) {
    return StringUtils.hasText(value)
        ? value.trim().toLowerCase(Locale.ROOT).replace('_', '-')
        : "";
  }

  /** Identifica modelos ou providers Luma Ray. */
  private boolean isLuma(String providerName, String model) {
    String normalizedProvider = normalize(providerName);
    String normalizedModel = normalize(model);
    return contains(normalizedProvider, "luma")
        || contains(normalizedProvider, "ray")
        || contains(normalizedModel, "luma")
        || contains(normalizedModel, "ray-3.2");
  }

  /** Identifica modelos ou providers Kling. */
  private boolean isKling(String normalizedProvider, String normalizedModel) {
    return contains(normalizedProvider, "kling") || contains(normalizedModel, "kling");
  }

  /** Identifica modelos ou providers Runway. */
  private boolean isRunway(String normalizedProvider, String normalizedModel) {
    return contains(normalizedProvider, "runway")
        || contains(normalizedProvider, "runaway")
        || contains(normalizedModel, "gen4.5")
        || contains(normalizedModel, "gen-4.5");
  }

  /** Identifica o Seedance 2 disponibilizado pelo contrato oficial da Runway. */
  private boolean isSeedance2(String normalizedProvider, String normalizedModel) {
    return contains(normalizedProvider, "seedance-2")
        || contains(normalizedProvider, "seedance2")
        || contains(normalizedModel, "seedance2")
        || contains(normalizedModel, "seedance-2");
  }

  /** Identifica o Hailuo 3 disponibilizado pelo contrato oficial da Runway. */
  private boolean isHailuo3(String normalizedProvider, String normalizedModel) {
    return contains(normalizedProvider, "hailuo-3")
        || contains(normalizedProvider, "hailuo3")
        || contains(normalizedModel, "hailuo3");
  }

  /** Identifica modelos ou providers HeyGen. */
  private boolean isHeyGen(String normalizedProvider, String normalizedModel) {
    return contains(normalizedProvider, "heygen") || contains(normalizedModel, "heygen");
  }

  /** Testa presença de um marcador normalizado. */
  private boolean contains(String value, String marker) {
    return value.contains(marker);
  }
}
