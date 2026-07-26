package com.marketinghub.product.service.organicvideoplan;

import java.util.List;

/** Responsabilidade: representar um vídeo planejado dentro do playbook orgânico do produto. */
public record ProductOrganicVideoPlanItemResponse(
    int day,
    int sequence,
    String category,
    String funnelStage,
    String mentalShift,
    String platformPriority,
    String hook,
    String scene,
    String message,
    String callToAction,
    String primaryMetric,
    List<String> productionNotes) {}
