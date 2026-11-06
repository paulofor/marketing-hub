package com.marketinghub.salesvideo.service.clipplan;

import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: expor duração final, tomadas cobradas e pendências antes da confirmação. */
public record StudioClipPlanResponse(
    String capabilityRevision,
    int finalSeconds,
    int generatedSeconds,
    int trimSeconds,
    List<Scene> scenes,
    BigDecimal clipsCostUsd,
    boolean productionAllowed,
    List<String> blockers) {
  /** Explica a função narrativa e os segundos usados e cobrados por tomada. */
  public record Scene(
      int order,
      String modelId,
      int requestedSeconds,
      int usedSeconds,
      int billedSeconds,
      BigDecimal maximumCostUsd,
      String narrativeRole) {}
}
