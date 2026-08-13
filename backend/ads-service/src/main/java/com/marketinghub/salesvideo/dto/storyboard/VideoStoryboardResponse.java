package com.marketinghub.salesvideo.dto.storyboard;

import java.util.List;

/** Responsabilidade: consolidar o storyboard auditável de um projeto do Estúdio. */
public record VideoStoryboardResponse(
    Long projectId,
    int plannedSceneCount,
    int expectedCredits,
    int consumedCredits,
    Integer utilizationPercent,
    List<VideoStoryboardSceneResponse> scenes) {}
