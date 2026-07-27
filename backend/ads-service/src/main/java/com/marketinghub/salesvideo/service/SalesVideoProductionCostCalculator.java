package com.marketinghub.salesvideo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Calcula o custo de produção de vídeos gerados por providers de IA. */
@Component
public class SalesVideoProductionCostCalculator {
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

  /** Verifica se a resolução solicitada é 4k. */
  private boolean is4k(String resolution) {
    String normalized = normalize(resolution);
    return normalized.contains("4k") || normalized.contains("2160");
  }

  /** Normaliza texto para comparação tolerante. */
  private String normalize(String value) {
    return StringUtils.hasText(value)
        ? value.trim().toLowerCase(Locale.ROOT).replace('_', '-')
        : "";
  }

  /** Testa presença de um marcador normalizado. */
  private boolean contains(String value, String marker) {
    return value.contains(marker);
  }
}
