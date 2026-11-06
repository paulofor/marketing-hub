package com.marketinghub.salesvideo.service;

import com.marketinghub.videocontract.v1.VideoCapabilities;
import java.util.Locale;
import java.util.Optional;

/** Centraliza os limites de duração aceitos por cada provider de vídeo comercial. */
public final class SalesVideoProviderDurationPolicy {
  private static final int KLING_MAX_SECONDS = 10;
  private static final int RUNWAY_MAX_SECONDS = 10;
  private static final int RUNWAY_PRODUCT_UGC_MAX_SECONDS = 15;
  private static final int VEO_MAX_SECONDS = 8;
  private static final int HEYGEN_MAX_SECONDS = 600;
  private static final int EDITORIAL_MOTION_MAX_SECONDS = 60;

  /** Impede instância de uma política puramente estática. */
  private SalesVideoProviderDurationPolicy() {}

  /** Retorna a violação de duração do provider ou vazio quando o pedido é válido. */
  public static Optional<String> validate(String providerName, Integer targetDurationSeconds) {
    if (targetDurationSeconds == null || targetDurationSeconds <= 0) {
      return Optional.empty();
    }
    var capability = VideoCapabilities.provider(providerName);
    if (capability.isPresent()) {
      try {
        capability.get().requireDuration(targetDurationSeconds);
        return Optional.empty();
      } catch (IllegalArgumentException ex) {
        return Optional.of(ex.getMessage());
      }
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

  /** Compartilha o limite por clipe entre preflight, painel e planejamento do ciclo. */
  public static int maxClipSecondsForPlan(String providerPlan) {
    String plan = Optional.ofNullable(providerPlan).orElse("").toUpperCase(Locale.ROOT);
    // A receita mantém o mesmo marcador explícito exigido pelo backend e pelo executor.
    if (plan.contains("(RUNWAY_PRODUCT_UGC)")) {
      return maxSeconds("RUNWAY_PRODUCT_UGC");
    }
    if (plan.contains("(EDITORIAL_MOTION)")) {
      return maxSeconds("EDITORIAL_MOTION");
    }
    return VideoCapabilities.selected(providerPlan)
        .map(VideoCapabilities.Model::maxSeconds)
        .orElse(RUNWAY_MAX_SECONDS);
  }

  /** Resolve aliases conhecidos dos providers integrados. */
  private static ProviderLimit resolveLimit(String providerName) {
    var capability = VideoCapabilities.provider(providerName);
    if (capability.isPresent())
      return new ProviderLimit(capability.get().displayName(), capability.get().maxSeconds());
    String normalized =
        Optional.ofNullable(providerName).map(String::trim).orElse("").toUpperCase(Locale.ROOT);
    if (!hasText(normalized)) {
      return null;
    }
    // Luma é executado pelo módulo de vídeo como montagem de cenas de dez segundos.
    // O limite do provider vale para cada cena, não para a duração do vídeo final.
    if (normalized.contains("LUMA") || normalized.contains("RAY_3_2")) return null;
    if (normalized.contains("KLING")) {
      return new ProviderLimit("Kling", KLING_MAX_SECONDS);
    }
    if (normalized.contains("EDITORIAL_MOTION") || normalized.contains("LOCAL_EDITORIAL")) {
      return new ProviderLimit("Movimento editorial local", EDITORIAL_MOTION_MAX_SECONDS);
    }
    if (normalized.contains("RUNWAY_PRODUCT_UGC") || normalized.contains("PRODUCT_UGC")) {
      return new ProviderLimit("Runway Product UGC", RUNWAY_PRODUCT_UGC_MAX_SECONDS);
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
