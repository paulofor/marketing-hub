package com.marketinghub.salesvideo.service;

import java.util.Locale;
import java.util.Optional;

/** Centraliza os limites de duração aceitos por cada provider de vídeo comercial. */
public final class SalesVideoProviderDurationPolicy {
  private static final int LUMA_RAY_3_2_MAX_SECONDS = 30;
  private static final int KLING_MAX_SECONDS = 10;
  private static final int RUNWAY_MAX_SECONDS = 10;
  private static final int VEO_MAX_SECONDS = 8;
  private static final int HEYGEN_MAX_SECONDS = 600;

  /** Impede instância de uma política puramente estática. */
  private SalesVideoProviderDurationPolicy() {}

  /** Retorna a violação de duração do provider ou vazio quando o pedido é válido. */
  public static Optional<String> validate(String providerName, Integer targetDurationSeconds) {
    if (targetDurationSeconds == null || targetDurationSeconds <= 0) {
      return Optional.empty();
    }
    ProviderLimit limit = resolveLimit(providerName);
    if (limit == null || targetDurationSeconds <= limit.maxSeconds()) {
      return Optional.empty();
    }
    return Optional.of(
        "%s aceita no máximo %d segundos por solicitação; use montagem por cenas ou outro provider para vídeos maiores"
            .formatted(limit.label(), limit.maxSeconds()));
  }

  /** Identifica o limite operacional do provider usado pelo Marketing Hub. */
  public static Integer maxSeconds(String providerName) {
    ProviderLimit limit = resolveLimit(providerName);
    return limit == null ? null : limit.maxSeconds();
  }

  /** Resolve aliases conhecidos dos providers integrados. */
  private static ProviderLimit resolveLimit(String providerName) {
    String normalized =
        Optional.ofNullable(providerName).map(String::trim).orElse("").toUpperCase(Locale.ROOT);
    if (!hasText(normalized)) {
      return null;
    }
    if (normalized.contains("LUMA") || normalized.contains("RAY_3_2")) {
      return new ProviderLimit("Luma Ray 3.2", LUMA_RAY_3_2_MAX_SECONDS);
    }
    if (normalized.contains("KLING")) {
      return new ProviderLimit("Kling", KLING_MAX_SECONDS);
    }
    if (normalized.contains("RUNWAY") || normalized.contains("RUNAWAY")) {
      return new ProviderLimit("Runway", RUNWAY_MAX_SECONDS);
    }
    if (normalized.equals("VEO")
        || normalized.contains("VEO-")
        || normalized.contains("VEO_")
        || normalized.contains("VEO ")) {
      return new ProviderLimit("VEO", VEO_MAX_SECONDS);
    }
    if (normalized.contains("HEYGEN")) {
      return new ProviderLimit("HeyGen", HEYGEN_MAX_SECONDS);
    }
    return null;
  }

  /** Verifica texto preenchido sem depender de utilitário Spring em classe estática simples. */
  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Guarda o nome comercial do provider e sua duração máxima operacional. */
  private record ProviderLimit(String label, int maxSeconds) {}
}
