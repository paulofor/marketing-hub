package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.VideoProjectStatus;
import jakarta.validation.constraints.Size;

/** Payload para edição de projeto de vídeo no estúdio. */
public record UpdateVideoProjectRequest(
    Long productId,
    Long experimentId,
    Long salesVideoProfileId,
    @Size(max = 191) String campaignKey,
    @Size(max = 64) String contextType,
    @Size(max = 64) String productionMode,
    @Size(max = 64) String targetChannel,
    @Size(max = 64) String format,
    @Size(max = 255) String title,
    @Size(max = 1024) String objective,
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
    Integer targetDurationSeconds,
    String providerPlan,
    String editingNotes,
    String qualityGate,
    VideoProjectStatus status,
    @Size(max = 191) String updatedBy) {}
