package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.repository.LandingPageRepository;
import com.marketinghub.product.Product;
import com.marketinghub.product.repository.ProductRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import com.marketinghub.salesvideo.repository.SalesVideoJobRepository;
import com.marketinghub.salesvideo.repository.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.repository.SalesVideoScriptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.PRODUCT_NOT_FOUND,
                        "Produto não encontrado: " + productId));
        LandingPage landingPage = null;
        if (request.getLandingPageId() != null) {
            landingPage = landingPageRepository.findById(request.getLandingPageId())
                    .orElseThrow(() -> VideoModuleException.badRequest(VideoModuleErrorCode.LANDING_NOT_FOUND,
                            "Landing page não encontrada: " + request.getLandingPageId()));
        }
        String tenantId = TenantContextHolder.requireTenant();
        String createdBy = TenantContextHolder.resolveUserEmail(null);
        SalesVideoProfile profile = SalesVideoProfile.builder()
                .product(product)
                .landingPage(landingPage)
                .tenantId(tenantId)
                .createdBy(createdBy)
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
        String tenantId = TenantContextHolder.requireTenant();
        return profileRepository.findByProductIdAndTenantIdOrderByCreatedAtDesc(productId, tenantId)
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
        String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
        SalesVideoJob job = jobService.createJob(profile,
                null,
                SalesVideoJobType.SCRIPT,
                SalesVideoProviderFamily.OPENAI,
                request.getProviderName(),
                requestedBy);
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
        String approvedBy = TenantContextHolder.resolveUserEmail(request.getApprovedBy());
        SalesVideoScript script = SalesVideoScript.builder()
                .profile(profile)
                .version(nextVersion)
                .createdBy(approvedBy)
                .scriptText(request.getScriptText())
                .hookText(request.getHookText())
                .ctaText(request.getCtaText())
                .captionText(request.getCaptionText())
                .source(SalesVideoScriptSource.MANUAL)
                .status(SalesVideoScriptStatus.APPROVED)
                .approvedBy(approvedBy)
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
                .orElseThrow(() -> VideoModuleException.badRequest(VideoModuleErrorCode.SCRIPT_NOT_FOUND,
                        "É necessário ter um script aprovado antes da renderização"));
        SalesVideoProviderFamily family = Optional.ofNullable(request.getProviderFamily())
                .orElse(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
        String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
        SalesVideoJob job = jobService.createJob(profile,
                script,
                SalesVideoJobType.RENDER,
                family,
                request.getProviderName(),
                requestedBy);
        profile.setStatus(SalesVideoStatus.VIDEO_REQUESTED);
        profileRepository.save(profile);
        return SalesVideoMapper.toDto(job);
    }

    @Transactional(readOnly = true)
    public List<SalesVideoScriptDto> listScripts(Long profileId) {
        SalesVideoProfile profile = loadProfile(profileId);
        return scriptRepository.findByProfileIdOrderByVersionDesc(profile.getId())
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    private SalesVideoProfile loadProfile(Long profileId) {
        SalesVideoProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Perfil de vídeo não encontrado: " + profileId));
        TenantContextHolder.assertTenant(profile.getTenantId());
        return profile;
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
