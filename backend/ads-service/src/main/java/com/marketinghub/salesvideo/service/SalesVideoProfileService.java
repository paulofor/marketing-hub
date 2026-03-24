package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.repository.LandingPageRepository;
import com.marketinghub.product.Product;
import com.marketinghub.product.repository.ProductRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.salesvideo.repository.SalesVideoJobRepository;
import com.marketinghub.salesvideo.repository.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.repository.SalesVideoScriptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra as operações administrativas sobre perfis de vídeo.
 */
@Service
public class SalesVideoProfileService {
    private final SalesVideoProfileRepository profileRepository;
    private final ProductRepository productRepository;
    private final LandingPageRepository landingPageRepository;
    private final SalesVideoScriptRepository scriptRepository;
    private final SalesVideoJobRepository jobRepository;
    private final SalesVideoJobService jobService;

    public SalesVideoProfileService(SalesVideoProfileRepository profileRepository,
                                    ProductRepository productRepository,
                                    LandingPageRepository landingPageRepository,
                                    SalesVideoScriptRepository scriptRepository,
                                    SalesVideoJobRepository jobRepository,
                                    SalesVideoJobService jobService) {
        this.profileRepository = profileRepository;
        this.productRepository = productRepository;
        this.landingPageRepository = landingPageRepository;
        this.scriptRepository = scriptRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
    }

    @Transactional
    public SalesVideoProfileDto createProfile(Long productId, CreateSalesVideoProfileRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produto não encontrado: " + productId));
        LandingPage landingPage = null;
        if (request.getLandingPageId() != null) {
            landingPage = landingPageRepository.findById(request.getLandingPageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Landing page não encontrada: " + request.getLandingPageId()));
        }
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .product(product)
                .landingPage(landingPage)
                .videoKind(request.getVideoKind())
                .title(request.getTitle())
                .personaName(request.getPersonaName())
                .personaStyle(request.getPersonaStyle())
                .voiceStyle(request.getVoiceStyle())
                .language(request.getLanguage())
                .targetDurationSeconds(request.getTargetDurationSeconds())
                .status(SalesVideoStatus.DRAFT)
                .build();
        SalesVideoProfile persisted = profileRepository.save(profile);
        return toDto(persisted);
    }

    @Transactional(readOnly = true)
    public List<SalesVideoProfileDto> listProfiles(Long productId) {
        return profileRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesVideoProfileDto getProfile(Long profileId) {
        return toDto(loadProfile(profileId));
    }

    @Transactional
    public SalesVideoJobDto requestScriptGeneration(Long profileId, GenerateSalesVideoScriptRequest request) {
        SalesVideoProfile profile = loadProfile(profileId);
        profile.setStatus(SalesVideoStatus.SCRIPT_PENDING);
        SalesVideoJob job = jobService.createJob(profile,
                null,
                SalesVideoJobType.SCRIPT,
                SalesVideoProviderFamily.OPENAI,
                request.getProviderName(),
                request.getRequestedBy());
        profileRepository.save(profile);
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoScriptDto approveScript(Long profileId, ApproveSalesVideoScriptRequest request) {
        SalesVideoProfile profile = loadProfile(profileId);
        int nextVersion = scriptRepository.findFirstByProfileIdOrderByVersionDesc(profileId)
                .map(SalesVideoScript::getVersion)
                .map(v -> v + 1)
                .orElse(1);
        SalesVideoScript script = SalesVideoScript.builder()
                .profile(profile)
                .version(nextVersion)
                .scriptText(request.getScriptText())
                .hookText(request.getHookText())
                .ctaText(request.getCtaText())
                .captionText(request.getCaptionText())
                .source(SalesVideoScriptSource.MANUAL)
                .status(SalesVideoScriptStatus.APPROVED)
                .approvedBy(request.getApprovedBy())
                .approvedAt(Instant.now())
                .build();
        SalesVideoScript saved = scriptRepository.save(script);
        profile.getScripts().add(saved);
        profile.setStatus(SalesVideoStatus.SCRIPT_READY);
        profileRepository.save(profile);
        return SalesVideoMapper.toDto(saved);
    }

    @Transactional
    public SalesVideoJobDto requestRender(Long profileId, RequestVideoRenderRequest request) {
        SalesVideoProfile profile = loadProfile(profileId);
        SalesVideoScript script = scriptRepository
                .findFirstByProfileIdAndStatusOrderByVersionDesc(profileId, SalesVideoScriptStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "É necessário ter um script aprovado antes da renderização"));
        SalesVideoProviderFamily family = Optional.ofNullable(request.getProviderFamily())
                .orElse(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
        SalesVideoJob job = jobService.createJob(profile,
                script,
                SalesVideoJobType.RENDER,
                family,
                request.getProviderName(),
                request.getRequestedBy());
        profile.setStatus(SalesVideoStatus.VIDEO_REQUESTED);
        profileRepository.save(profile);
        return SalesVideoMapper.toDto(job);
    }

    private SalesVideoProfile loadProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Perfil de vídeo não encontrado: " + profileId));
    }

    private SalesVideoProfileDto toDto(SalesVideoProfile profile) {
        SalesVideoScript latestScript = scriptRepository
                .findFirstByProfileIdOrderByVersionDesc(profile.getId())
                .orElse(null);
        SalesVideoJob lastJob = jobRepository
                .findFirstByProfileIdOrderByRequestedAtDesc(profile.getId())
                .orElse(null);
        return SalesVideoMapper.toDto(profile, latestScript, lastJob);
    }
}
