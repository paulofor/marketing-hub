package com.marketinghub.salesvideo.dto.storyboard;

/** Responsabilidade: expor uma cena planejada e suas tentativas produtivas e financeiras. */
public record VideoStoryboardSceneResponse(
    Long consumptionId,
    int sceneNumber,
    String commercialRole,
    String plan,
    Long jobId,
    String jobStatus,
    String providerTaskId,
    Integer requestedDurationSeconds,
    Integer expectedCredits,
    Integer consumedCredits,
    String producedFileUrl,
    Integer utilizationPercent,
    String utilizationEvidence,
    String commercialEvaluationStatus,
    String commercialEvaluationNotes,
    String commercialEvaluatedBy,
    java.time.Instant commercialEvaluatedAt) {}
