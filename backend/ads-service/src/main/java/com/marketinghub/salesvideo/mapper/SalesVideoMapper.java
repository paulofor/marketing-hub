package com.marketinghub.salesvideo.mapper;

import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;

/**
 * Conversões entre entidades e DTOs do módulo de vídeo.
 */
public final class SalesVideoMapper {
    private SalesVideoMapper() {
    }

    public static SalesVideoProfileDto toDto(SalesVideoProfile profile,
                                             SalesVideoScript latestScript,
                                             SalesVideoJob lastJob) {
        if (profile == null) {
            return null;
        }
        SalesVideoProfileDto dto = new SalesVideoProfileDto();
        dto.setId(profile.getId());
        dto.setProductId(profile.getProduct() != null ? profile.getProduct().getId() : null);
        dto.setLandingPageId(profile.getLandingPage() != null ? profile.getLandingPage().getId() : null);
        dto.setVideoKind(profile.getVideoKind());
        dto.setTitle(profile.getTitle());
        dto.setPersonaName(profile.getPersonaName());
        dto.setPersonaStyle(profile.getPersonaStyle());
        dto.setVoiceStyle(profile.getVoiceStyle());
        dto.setLanguage(profile.getLanguage());
        dto.setTargetDurationSeconds(profile.getTargetDurationSeconds());
        dto.setStatus(profile.getStatus());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setLatestScript(toDto(latestScript));
        dto.setLastJob(toDto(lastJob));
        return dto;
    }

    public static SalesVideoScriptDto toDto(SalesVideoScript script) {
        if (script == null) {
            return null;
        }
        SalesVideoScriptDto dto = new SalesVideoScriptDto();
        dto.setId(script.getId());
        dto.setVersion(script.getVersion());
        dto.setScriptText(script.getScriptText());
        dto.setHookText(script.getHookText());
        dto.setCtaText(script.getCtaText());
        dto.setCaptionText(script.getCaptionText());
        dto.setStoryboardJson(script.getStoryboardJson());
        dto.setSource(script.getSource());
        dto.setModel(script.getModel());
        dto.setPrompt(script.getPrompt());
        dto.setStatus(script.getStatus());
        dto.setApprovedBy(script.getApprovedBy());
        dto.setApprovedAt(script.getApprovedAt());
        dto.setCreatedAt(script.getCreatedAt());
        return dto;
    }

    public static SalesVideoJobDto toDto(SalesVideoJob job) {
        if (job == null) {
            return null;
        }
        SalesVideoJobDto dto = new SalesVideoJobDto();
        dto.setId(job.getId());
        dto.setProfileId(job.getProfile() != null ? job.getProfile().getId() : null);
        dto.setScriptId(job.getScript() != null ? job.getScript().getId() : null);
        dto.setProviderFamily(job.getProviderFamily());
        dto.setProviderName(job.getProviderName());
        dto.setProviderJobId(job.getProviderJobId());
        dto.setJobType(job.getJobType());
        dto.setStatus(job.getStatus());
        dto.setProgressPercent(job.getProgressPercent());
        dto.setFailureCode(job.getFailureCode());
        dto.setFailureDetail(job.getFailureDetail());
        dto.setRequestedBy(job.getRequestedBy());
        dto.setRequestedAt(job.getRequestedAt());
        dto.setStartedAt(job.getStartedAt());
        dto.setFinishedAt(job.getFinishedAt());
        dto.setExpiresAt(job.getExpiresAt());
        dto.setAssetId(job.getAsset() != null ? job.getAsset().getId() : null);
        dto.setPosterAssetId(job.getPosterAsset() != null ? job.getPosterAsset().getId() : null);
        dto.setVttAssetId(job.getVttAsset() != null ? job.getVttAsset().getId() : null);
        dto.setMetadataJson(job.getMetadataJson());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }

    public static SalesVideoJobEventDto toDto(SalesVideoJobEvent event) {
        if (event == null) {
            return null;
        }
        SalesVideoJobEventDto dto = new SalesVideoJobEventDto();
        dto.setId(event.getId());
        dto.setEventType(event.getEventType());
        dto.setOldStatus(event.getOldStatus());
        dto.setNewStatus(event.getNewStatus());
        dto.setMessage(event.getMessage());
        dto.setDetailsJson(event.getDetailsJson());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    public static LandingVideoSlotDto toDto(LandingVideoSlot slot) {
        if (slot == null) {
            return null;
        }
        LandingVideoSlotDto dto = new LandingVideoSlotDto();
        dto.setId(slot.getId());
        dto.setLandingPageId(slot.getLandingPage() != null ? slot.getLandingPage().getId() : null);
        dto.setProfileId(slot.getProfile() != null ? slot.getProfile().getId() : null);
        dto.setSlotName(slot.getSlotName());
        dto.setAssetId(slot.getAsset() != null ? slot.getAsset().getId() : null);
        dto.setPosterAssetId(slot.getPosterAsset() != null ? slot.getPosterAsset().getId() : null);
        dto.setVttAssetId(slot.getVttAsset() != null ? slot.getVttAsset().getId() : null);
        dto.setAutoplay(slot.isAutoplay());
        dto.setMuted(slot.isMuted());
        dto.setLoopVideo(slot.isLoopVideo());
        dto.setControlsEnabled(slot.isControlsEnabled());
        dto.setLazyLoad(slot.isLazyLoad());
        dto.setPublishedAt(slot.getPublishedAt());
        dto.setPublishedBy(slot.getPublishedBy());
        dto.setCreatedAt(slot.getCreatedAt());
        dto.setUpdatedAt(slot.getUpdatedAt());
        return dto;
    }
}
