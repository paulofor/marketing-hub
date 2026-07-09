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
        dto.setTenantId(profile.getTenantId());
        dto.setCreatedBy(profile.getCreatedBy());
        dto.setVideoKind(profile.getVideoKind());
        dto.setTitle(profile.getTitle());
        dto.setPersonaName(profile.getPersonaName());
        dto.setPersonaStyle(profile.getPersonaStyle());
        dto.setVoiceStyle(profile.getVoiceStyle());
        dto.setLanguage(profile.getLanguage());
        dto.setTargetDurationSeconds(profile.getTargetDurationSeconds());
        dto.setRequiresConsent(profile.isRequiresConsent());
        dto.setConsentRecordedBy(profile.getConsentRecordedBy());
        dto.setConsentRecordedAt(profile.getConsentRecordedAt());
        dto.setConsentEvidenceUrl(profile.getConsentEvidenceUrl());
        dto.setHumanReviewApprovedBy(profile.getHumanReviewApprovedBy());
        dto.setHumanReviewApprovedAt(profile.getHumanReviewApprovedAt());
        dto.setComplianceNotes(profile.getComplianceNotes());
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
        dto.setCreatedBy(script.getCreatedBy());
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
        dto.setTenantId(job.getTenantId());
        dto.setProviderFamily(job.getProviderFamily());
        dto.setExecutionMode(job.getExecutionMode());
        dto.setProviderName(job.getProviderName());
        dto.setProviderJobId(job.getProviderJobId());
        dto.setJobType(job.getJobType());
        dto.setStatus(job.getStatus());
        dto.setRetryAttempt(job.getRetryAttempt());
        dto.setRetryReason(job.getRetryReason());
        dto.setRetryOfJobId(job.getRetryOfJob() != null ? job.getRetryOfJob().getId() : null);
        dto.setRetryNotes(job.getRetryNotes());
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
        dto.setAssetUrl(job.getAsset() != null ? job.getAsset().getUrl() : null);
        dto.setPosterAssetUrl(job.getPosterAsset() != null ? job.getPosterAsset().getUrl() : null);
        dto.setVttAssetUrl(job.getVttAsset() != null ? job.getVttAsset().getUrl() : null);
        dto.setMetadataJson(job.getMetadataJson());
        dto.setAuditSnapshotJson(job.getAuditSnapshotJson());
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
        dto.setTenantId(slot.getTenantId());
        dto.setSlotName(slot.getSlotName());
        dto.setAssetId(slot.getAsset() != null ? slot.getAsset().getId() : null);
        dto.setPosterAssetId(slot.getPosterAsset() != null ? slot.getPosterAsset().getId() : null);
        dto.setVttAssetId(slot.getVttAsset() != null ? slot.getVttAsset().getId() : null);
        dto.setAssetUrl(slot.getAsset() != null ? slot.getAsset().getUrl() : null);
        dto.setPosterAssetUrl(slot.getPosterAsset() != null ? slot.getPosterAsset().getUrl() : null);
        dto.setVttAssetUrl(slot.getVttAsset() != null ? slot.getVttAsset().getUrl() : null);
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
    public static LandingVideoSlotHistoryDto toDto(LandingVideoSlotHistory history) {
        if (history == null) {
            return null;
        }
        LandingVideoSlotHistoryDto dto = new LandingVideoSlotHistoryDto();
        dto.setId(history.getId());
        dto.setSlotId(history.getSlot() != null ? history.getSlot().getId() : null);
        dto.setProfileId(history.getProfile() != null ? history.getProfile().getId() : null);
        dto.setLandingPageId(history.getLandingPage() != null ? history.getLandingPage().getId() : null);
        dto.setTenantId(history.getTenantId());
        dto.setSlotName(history.getSlotName());
        dto.setAssetId(history.getAsset() != null ? history.getAsset().getId() : null);
        dto.setPosterAssetId(history.getPosterAsset() != null ? history.getPosterAsset().getId() : null);
        dto.setVttAssetId(history.getVttAsset() != null ? history.getVttAsset().getId() : null);
        dto.setAutoplay(history.isAutoplay());
        dto.setMuted(history.isMuted());
        dto.setLoopVideo(history.isLoopVideo());
        dto.setControlsEnabled(history.isControlsEnabled());
        dto.setLazyLoad(history.isLazyLoad());
        dto.setChangeType(history.getChangeType());
        dto.setChangedBy(history.getChangedBy());
        dto.setChangedAt(history.getChangedAt());
        dto.setPublishedBy(history.getPublishedBy());
        dto.setPublishedAt(history.getPublishedAt());
        dto.setNotes(history.getNotes());
        return dto;
    }

}
