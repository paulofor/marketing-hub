package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.repository.LandingPageRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.dto.CreateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.salesvideo.dto.UpdateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.salesvideo.repository.LandingVideoSlotRepository;
import com.marketinghub.salesvideo.repository.SalesVideoProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Gerencia os slots de publicação de vídeo nas landing pages.
 */
@Service
public class LandingVideoSlotService {
    private final LandingVideoSlotRepository slotRepository;
    private final LandingPageRepository landingPageRepository;
    private final SalesVideoProfileRepository profileRepository;
    private final AssetRepository assetRepository;

    public LandingVideoSlotService(LandingVideoSlotRepository slotRepository,
                                   LandingPageRepository landingPageRepository,
                                   SalesVideoProfileRepository profileRepository,
                                   AssetRepository assetRepository) {
        this.slotRepository = slotRepository;
        this.landingPageRepository = landingPageRepository;
        this.profileRepository = profileRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional
    public LandingVideoSlotDto create(Long landingId, CreateLandingVideoSlotRequest request) {
        LandingPage landingPage = landingPageRepository.findById(landingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Landing page não encontrada: " + landingId));
        SalesVideoProfile profile = profileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Perfil não encontrado: " + request.getProfileId()));
        slotRepository.findByLandingPageIdAndSlotName(landingId, request.getSlotName())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Slot já configurado para esta landing: " + request.getSlotName());
                });
        LandingVideoSlot slot = LandingVideoSlot.builder()
                .landingPage(landingPage)
                .profile(profile)
                .slotName(request.getSlotName())
                .asset(loadAsset(request.getAssetId()))
                .posterAsset(loadOptionalAsset(request.getPosterAssetId()))
                .vttAsset(loadOptionalAsset(request.getVttAssetId()))
                .autoplay(request.isAutoplay())
                .muted(request.isMuted())
                .loopVideo(request.isLoopVideo())
                .controlsEnabled(request.isControlsEnabled())
                .lazyLoad(request.isLazyLoad())
                .publishedBy(request.getPublishedBy())
                .publishedAt(request.getPublishedBy() != null ? Instant.now() : null)
                .build();
        LandingVideoSlot saved = slotRepository.save(slot);
        return SalesVideoMapper.toDto(saved);
    }

    @Transactional
    public LandingVideoSlotDto update(Long landingId, Long slotId, UpdateLandingVideoSlotRequest request) {
        LandingVideoSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Slot não encontrado: " + slotId));
        if (!slot.getLandingPage().getId().equals(landingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot não pertence à landing solicitada");
        }
        if (request.getProfileId() != null) {
            SalesVideoProfile profile = profileRepository.findById(request.getProfileId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Perfil não encontrado: " + request.getProfileId()));
            slot.setProfile(profile);
        }
        if (request.getSlotName() != null) {
            slotRepository.findByLandingPageIdAndSlotName(landingId, request.getSlotName())
                    .filter(existing -> !existing.getId().equals(slot.getId()))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Outro slot já usa este nome na mesma landing");
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
        if (request.getPublishedBy() != null) {
            slot.setPublishedBy(request.getPublishedBy());
            slot.setPublishedAt(Instant.now());
        }
        LandingVideoSlot saved = slotRepository.save(slot);
        return SalesVideoMapper.toDto(saved);
    }

    private Asset loadAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Asset não encontrado: " + id));
    }

    private Asset loadOptionalAsset(Long id) {
        if (id == null) {
            return null;
        }
        return loadAsset(id);
    }
}
