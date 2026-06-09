package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.CreateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.salesvideo.dto.LandingVideoSlotHistoryDto;
import com.marketinghub.salesvideo.dto.UpdateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotHistoryRepository;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * Gerencia os slots de publicação de vídeo nas landing pages.
 */
@Component
public class LandingVideoSlotService {
    private final LandingVideoSlotRepository slotRepository;
    private final LandingPageRepository landingPageRepository;
    private final SalesVideoProfileRepository profileRepository;
    private final AssetRepository assetRepository;
    private final LandingVideoSlotHistoryRepository historyRepository;

    public LandingVideoSlotService(LandingVideoSlotRepository slotRepository,
                                   LandingPageRepository landingPageRepository,
                                   SalesVideoProfileRepository profileRepository,
                                   AssetRepository assetRepository,
                                   LandingVideoSlotHistoryRepository historyRepository) {
        this.slotRepository = slotRepository;
        this.landingPageRepository = landingPageRepository;
        this.profileRepository = profileRepository;
        this.assetRepository = assetRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public LandingVideoSlotDto create(Long landingId, CreateLandingVideoSlotRequest request) {
        LandingPage landingPage = landingPageRepository.findById(landingId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.LANDING_NOT_FOUND,
                        "Landing page não encontrada: " + landingId));
        SalesVideoProfile profile = loadProfile(request.getProfileId());
        TenantContextHolder.assertTenant(profile.getTenantId());
        ensurePublicationCompliance(profile);
        slotRepository.findByLandingPageIdAndSlotName(landingId, request.getSlotName())
                .ifPresent(existing -> {
                    throw VideoModuleException.conflict(VideoModuleErrorCode.SLOT_CONFLICT,
                            "Já existe um slot com esse nome na landing");
                });
        Asset asset = loadAsset(request.getAssetId());
        Asset poster = loadOptionalAsset(request.getPosterAssetId());
        Asset vtt = loadOptionalAsset(request.getVttAssetId());
        String publishedBy = resolveActor(request.getPublishedBy());
        LandingVideoSlot slot = LandingVideoSlot.builder()
                .landingPage(landingPage)
                .profile(profile)
                .tenantId(profile.getTenantId())
                .slotName(request.getSlotName())
                .asset(asset)
                .posterAsset(poster)
                .vttAsset(vtt)
                .autoplay(request.isAutoplay())
                .muted(request.isMuted())
                .loopVideo(request.isLoopVideo())
                .controlsEnabled(request.isControlsEnabled())
                .lazyLoad(request.isLazyLoad())
                .publishedBy(StringUtils.hasText(publishedBy) ? publishedBy : null)
                .publishedAt(StringUtils.hasText(publishedBy) ? Instant.now() : null)
                .build();
        LandingVideoSlot saved = slotRepository.save(slot);
        recordHistory(saved,
                StringUtils.hasText(publishedBy) ? LandingVideoSlotChangeType.PUBLISHED : LandingVideoSlotChangeType.CREATED,
                null,
                publishedBy);
        return SalesVideoMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LandingVideoSlotDto> list(Long landingId) {
        LandingPage landingPage = landingPageRepository.findById(landingId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.LANDING_NOT_FOUND,
                        "Landing page não encontrada: " + landingId));
        String tenantId = TenantContextHolder.requireTenant();
        return slotRepository.findByLandingPageIdAndTenantId(landingPage.getId(), tenantId)
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    @Transactional
    public LandingVideoSlotDto update(Long landingId, Long slotId, UpdateLandingVideoSlotRequest request) {
        LandingVideoSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.SLOT_NOT_FOUND,
                        "Slot não encontrado: " + slotId));
        ensureSameLanding(slot, landingId);
        TenantContextHolder.assertTenant(slot.getTenantId());

        if (request.getProfileId() != null && (slot.getProfile() == null
                || !slot.getProfile().getId().equals(request.getProfileId()))) {
            SalesVideoProfile profile = loadProfile(request.getProfileId());
            TenantContextHolder.assertTenant(profile.getTenantId());
            ensurePublicationCompliance(profile);
            slot.setProfile(profile);
            slot.setTenantId(profile.getTenantId());
        }
        if (StringUtils.hasText(request.getSlotName()) && !request.getSlotName().equals(slot.getSlotName())) {
            slotRepository.findByLandingPageIdAndSlotName(landingId, request.getSlotName())
                    .filter(existing -> !existing.getId().equals(slot.getId()))
                    .ifPresent(existing -> {
                        throw VideoModuleException.conflict(VideoModuleErrorCode.SLOT_CONFLICT,
                                "Já existe um slot com esse nome");
                    });
            slot.setSlotName(request.getSlotName());
        }
        if (request.getAssetId() != null) {
            slot.setAsset(loadAsset(request.getAssetId()));
        }
        if (request.getPosterAssetId() != null) {
            slot.setPosterAsset(loadOptionalAsset(request.getPosterAssetId()));
        }
        if (request.getVttAssetId() != null) {
            slot.setVttAsset(loadOptionalAsset(request.getVttAssetId()));
        }
        if (request.getAutoplay() != null) {
            slot.setAutoplay(request.getAutoplay());
        }
        if (request.getMuted() != null) {
            slot.setMuted(request.getMuted());
        }
        if (request.getLoopVideo() != null) {
            slot.setLoopVideo(request.getLoopVideo());
        }
        if (request.getControlsEnabled() != null) {
            slot.setControlsEnabled(request.getControlsEnabled());
        }
        if (request.getLazyLoad() != null) {
            slot.setLazyLoad(request.getLazyLoad());
        }

        LandingVideoSlotChangeType changeType = LandingVideoSlotChangeType.UPDATED;
        String actor = resolveActor(null);
        if (request.getPublishedBy() != null) {
            if (StringUtils.hasText(request.getPublishedBy())) {
                ensurePublicationCompliance(slot.getProfile());
                String publishedBy = resolveActor(request.getPublishedBy());
                slot.setPublishedBy(publishedBy);
                slot.setPublishedAt(Instant.now());
                changeType = LandingVideoSlotChangeType.PUBLISHED;
                actor = publishedBy;
            } else {
                slot.setPublishedBy(null);
                slot.setPublishedAt(null);
                changeType = LandingVideoSlotChangeType.UNPUBLISHED;
            }
        }

        LandingVideoSlot saved = slotRepository.save(slot);
        recordHistory(saved, changeType, null, actor);
        return SalesVideoMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LandingVideoSlotHistoryDto> history(Long landingId, Long slotId) {
        LandingVideoSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.SLOT_NOT_FOUND,
                        "Slot não encontrado: " + slotId));
        ensureSameLanding(slot, landingId);
        TenantContextHolder.assertTenant(slot.getTenantId());
        return historyRepository.findBySlotIdOrderByChangedAtDesc(slotId)
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    private SalesVideoProfile loadProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Perfil não encontrado: " + profileId));
    }

    private Asset loadAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> VideoModuleException.badRequest(VideoModuleErrorCode.ASSET_NOT_FOUND,
                        "Asset não encontrado: " + id));
    }

    private Asset loadOptionalAsset(Long id) {
        if (id == null) {
            return null;
        }
        return loadAsset(id);
    }

    private void ensureSameLanding(LandingVideoSlot slot, Long landingId) {
        if (!slot.getLandingPage().getId().equals(landingId)) {
            throw VideoModuleException.badRequest(VideoModuleErrorCode.SLOT_TENANT_MISMATCH,
                    "Slot não pertence à landing solicitada");
        }
    }

    private void recordHistory(LandingVideoSlot slot,
                               LandingVideoSlotChangeType changeType,
                               String notes,
                               String actor) {
        LandingVideoSlotHistory history = LandingVideoSlotHistory.builder()
                .slot(slot)
                .tenantId(slot.getTenantId())
                .profile(slot.getProfile())
                .landingPage(slot.getLandingPage())
                .slotName(slot.getSlotName())
                .asset(slot.getAsset())
                .posterAsset(slot.getPosterAsset())
                .vttAsset(slot.getVttAsset())
                .autoplay(slot.isAutoplay())
                .muted(slot.isMuted())
                .loopVideo(slot.isLoopVideo())
                .controlsEnabled(slot.isControlsEnabled())
                .lazyLoad(slot.isLazyLoad())
                .changeType(changeType)
                .changedBy(actor)
                .publishedBy(slot.getPublishedBy())
                .publishedAt(slot.getPublishedAt())
                .notes(notes)
                .build();
        historyRepository.save(history);
    }

    private String resolveActor(String candidate) {
        return TenantContextHolder.resolveUserEmail(candidate);
    }

    private void ensurePublicationCompliance(SalesVideoProfile profile) {
        if (profile == null) {
            return;
        }
        if (!StringUtils.hasText(profile.getHumanReviewApprovedBy()) || profile.getHumanReviewApprovedAt() == null) {
            throw VideoModuleException.conflict(VideoModuleErrorCode.COMPLIANCE_HUMAN_REVIEW_REQUIRED,
                    "Publicação bloqueada: revisão humana obrigatória para publicar vídeo na landing.");
        }
        if (profile.isRequiresConsent()
                && (!StringUtils.hasText(profile.getConsentRecordedBy())
                || profile.getConsentRecordedAt() == null
                || !StringUtils.hasText(profile.getConsentEvidenceUrl()))) {
            throw VideoModuleException.conflict(VideoModuleErrorCode.COMPLIANCE_CONSENT_REQUIRED,
                    "Publicação bloqueada: consentimento auditável obrigatório para avatar pessoal.");
        }
    }
}
