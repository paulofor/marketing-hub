package com.marketinghub.salesvideo.service;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orquestra as operações administrativas sobre perfis de vídeo.
 */
@Service
public class SalesVideoProfileService {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private final SalesVideoProfileRepository profileRepository;
    private final ProductRepository productRepository;
    private final LandingPageRepository landingPageRepository;
    private final SalesVideoScriptRepository scriptRepository;
    private final SalesVideoJobRepository jobRepository;
    private final SalesVideoJobService jobService;
    private final SalesVideoRolloutService rolloutService;

    public SalesVideoProfileService(SalesVideoProfileRepository profileRepository,
                                    ProductRepository productRepository,
                                    LandingPageRepository landingPageRepository,
                                    SalesVideoScriptRepository scriptRepository,
                                    SalesVideoJobRepository jobRepository,
                                    SalesVideoJobService jobService,
                                    SalesVideoRolloutService rolloutService) {
        this.profileRepository = profileRepository;
        this.productRepository = productRepository;
        this.landingPageRepository = landingPageRepository;
        this.scriptRepository = scriptRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.rolloutService = rolloutService;
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
                requestedBy,
                SalesVideoExecutionMode.TEST);
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
        SalesVideoExecutionMode executionMode = rolloutService.normalizeExecutionMode(request.getExecutionMode());
        if (executionMode == null) {
            executionMode = Optional.ofNullable(request.getExecutionMode()).orElse(SalesVideoExecutionMode.TEST);
        }
        if (executionMode == SalesVideoExecutionMode.PRODUCTION) {
            rolloutService.assertProductionRolloutAllowed(profile);
            ensureProductionCompliance(profile);
        }
        SalesVideoProviderFamily family = Optional.ofNullable(request.getProviderFamily())
                .orElse(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
        String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
        SalesVideoJob job = jobService.createJob(profile,
                script,
                SalesVideoJobType.RENDER,
                family,
                request.getProviderName(),
                requestedBy,
                executionMode);
        job.setAuditSnapshotJson(buildAuditSnapshot(profile, script, job, requestedBy));
        jobRepository.save(job);
        profile.setStatus(SalesVideoStatus.VIDEO_REQUESTED);
        profileRepository.save(profile);
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoProfileDto updateCompliance(Long profileId, UpdateSalesVideoComplianceRequest request) {
        SalesVideoProfile profile = loadProfile(profileId);
        if (request.getRequiresConsent() != null) {
            profile.setRequiresConsent(request.getRequiresConsent());
            if (!request.getRequiresConsent()) {
                profile.setConsentRecordedBy(null);
                profile.setConsentRecordedAt(null);
                profile.setConsentEvidenceUrl(null);
            }
        }
        if (StringUtils.hasText(request.getConsentRecordedBy())) {
            profile.setConsentRecordedBy(TenantContextHolder.resolveUserEmail(request.getConsentRecordedBy()));
            profile.setConsentRecordedAt(Instant.now());
        }
        if (request.getConsentEvidenceUrl() != null) {
            profile.setConsentEvidenceUrl(StringUtils.hasText(request.getConsentEvidenceUrl())
                    ? request.getConsentEvidenceUrl().trim()
                    : null);
        }
        if (request.getHumanReviewApproved() != null) {
            if (request.getHumanReviewApproved()) {
                String reviewer = TenantContextHolder.resolveUserEmail(request.getHumanReviewApprovedBy());
                if (!StringUtils.hasText(reviewer)) {
                    throw VideoModuleException.badRequest(VideoModuleErrorCode.COMPLIANCE_HUMAN_REVIEW_REQUIRED,
                            "Informe o responsável pela revisão humana.");
                }
                profile.setHumanReviewApprovedBy(reviewer);
                profile.setHumanReviewApprovedAt(Instant.now());
            } else {
                profile.setHumanReviewApprovedBy(null);
                profile.setHumanReviewApprovedAt(null);
            }
        }
        if (request.getComplianceNotes() != null) {
            profile.setComplianceNotes(StringUtils.hasText(request.getComplianceNotes())
                    ? request.getComplianceNotes().trim()
                    : null);
        }
        return toDto(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<SalesVideoScriptDto> listScripts(Long profileId) {
        SalesVideoProfile profile = loadProfile(profileId);
        return scriptRepository.findByProfileIdOrderByVersionDesc(profile.getId())
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesVideoRolloutStatusDto getRolloutStatus(Long profileId) {
        SalesVideoProfile profile = loadProfile(profileId);
        return rolloutService.evaluate(profile.getId(), profile.getTenantId());
    }

    @Transactional(readOnly = true)
    public SalesVideoRolloutStatusDto getTenantRolloutStatus() {
        String tenantId = TenantContextHolder.requireTenant();
        return rolloutService.evaluate(null, tenantId);
    }

    private SalesVideoProfile loadProfile(Long profileId) {
        SalesVideoProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Perfil de vídeo não encontrado: " + profileId));
        TenantContextHolder.assertTenant(profile.getTenantId());
        return profile;
    }

    private void ensureProductionCompliance(SalesVideoProfile profile) {
        if (profile.isRequiresConsent()) {
            if (!StringUtils.hasText(profile.getConsentRecordedBy())
                    || profile.getConsentRecordedAt() == null
                    || !StringUtils.hasText(profile.getConsentEvidenceUrl())) {
                throw VideoModuleException.conflict(VideoModuleErrorCode.COMPLIANCE_CONSENT_REQUIRED,
                        "Render produtivo bloqueado: registre consentimento auditável antes de continuar.");
            }
        }
        if (!StringUtils.hasText(profile.getHumanReviewApprovedBy()) || profile.getHumanReviewApprovedAt() == null) {
            throw VideoModuleException.conflict(VideoModuleErrorCode.COMPLIANCE_HUMAN_REVIEW_REQUIRED,
                    "Render produtivo bloqueado: revisão humana obrigatória antes da publicação.");
        }
    }

    private String buildAuditSnapshot(SalesVideoProfile profile,
                                      SalesVideoScript script,
                                      SalesVideoJob job,
                                      String requestedBy) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("capturedAt", Instant.now());
        snapshot.put("requestedBy", requestedBy);
        snapshot.put("profileId", profile.getId());
        snapshot.put("scriptId", script.getId());
        snapshot.put("scriptVersion", script.getVersion());
        snapshot.put("scriptSource", script.getSource());
        snapshot.put("scriptModel", script.getModel());
        snapshot.put("scriptPrompt", script.getPrompt());
        snapshot.put("providerFamily", job.getProviderFamily());
        snapshot.put("providerName", job.getProviderName());
        snapshot.put("executionMode", job.getExecutionMode());
        snapshot.put("requiresConsent", profile.isRequiresConsent());
        snapshot.put("consentRecordedBy", profile.getConsentRecordedBy());
        snapshot.put("consentRecordedAt", profile.getConsentRecordedAt());
        snapshot.put("consentEvidenceUrl", profile.getConsentEvidenceUrl());
        snapshot.put("humanReviewApprovedBy", profile.getHumanReviewApprovedBy());
        snapshot.put("humanReviewApprovedAt", profile.getHumanReviewApprovedAt());
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw VideoModuleException.internal(VideoModuleErrorCode.INTERNAL_ERROR,
                    "Falha ao serializar snapshot de auditoria do render.");
        }
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
