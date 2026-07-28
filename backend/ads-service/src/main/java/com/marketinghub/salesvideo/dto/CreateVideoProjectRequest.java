package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.VideoProjectStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload para criação de projeto de vídeo no estúdio. */
public record CreateVideoProjectRequest(
    Long productId,
    Long experimentId,
    Long salesVideoProfileId,
    @Size(max = 191) String campaignKey,
    @NotBlank @Size(max = 64) String contextType,
    @NotBlank @Size(max = 64) String productionMode,
    @NotBlank @Size(max = 64) String targetChannel,
    @NotBlank @Size(max = 64) String format,
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 1024) String objective,
    String storyText,
    @Size(max = 64) String funnelStage,
    @Size(max = 191) String primaryMetric,
    @Size(max = 1024) String hookText,
    String scriptText,
    String scenePlan,
    String visualReferences,
    String voiceoverPlan,
    String soundtrackPlan,
    String captionPlan,
    @Size(max = 1024) String ctaText,
    @Min(180) Integer targetDurationSeconds,
    String providerPlan,
    String editingNotes,
    String qualityGate,
    VideoProjectStatus status,
    @Size(max = 191) String createdBy) {}
