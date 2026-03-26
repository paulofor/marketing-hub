package com.marketinghub.salesvideo.service;

import com.marketinghub.media.Asset;
import com.marketinghub.media.repository.AssetRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.salesvideo.repository.SalesVideoJobEventRepository;
import com.marketinghub.salesvideo.repository.SalesVideoJobRepository;
import com.marketinghub.salesvideo.repository.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.repository.SalesVideoScriptRepository;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Contém as regras de negócio relativas aos jobs do módulo de vídeo.
 */
@Service
public class SalesVideoJobService {
    private static final int MAX_PAGE_SIZE = 200;

    private final SalesVideoJobRepository jobRepository;
    private final SalesVideoJobEventRepository eventRepository;
    private final SalesVideoProfileRepository profileRepository;
    private final SalesVideoScriptRepository scriptRepository;
    private final AssetRepository assetRepository;
    private final SalesVideoReprocessPolicy reprocessPolicy;

    public SalesVideoJobService(SalesVideoJobRepository jobRepository,
                                SalesVideoJobEventRepository eventRepository,
                                SalesVideoProfileRepository profileRepository,
                                SalesVideoScriptRepository scriptRepository,
                                AssetRepository assetRepository,
                                SalesVideoReprocessPolicy reprocessPolicy) {
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
        this.profileRepository = profileRepository;
        this.scriptRepository = scriptRepository;
        this.assetRepository = assetRepository;
        this.reprocessPolicy = reprocessPolicy;
    }

    @Transactional
    public SalesVideoJob createJob(SalesVideoProfile profile,
                                   SalesVideoScript script,
                                   SalesVideoJobType jobType,
                                   SalesVideoProviderFamily providerFamily,
                                   String providerName,
                                   String requestedBy) {
        SalesVideoStatus initialStatus = initialStatus(jobType);
        SalesVideoJob job = SalesVideoJob.builder()
                .profile(profile)
                .tenantId(profile != null ? profile.getTenantId() : TenantContextHolder.currentTenant())
                .script(script)
                .jobType(jobType)
                .providerFamily(providerFamily)
                .providerName(providerName)
                .status(initialStatus)
                .requestedBy(requestedBy)
                .requestedAt(Instant.now())
                .build();
        SalesVideoJob saved = jobRepository.save(job);
        registerEvent(saved, SalesVideoJobEventType.CREATED, null, initialStatus,
                "Job criado", null);
        maybeUpdateProfileStatus(saved, initialStatus);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SalesVideoJobDto> findJobs(SalesVideoProviderFamily providerFamily,
                                           SalesVideoStatus status,
                                           SalesVideoJobType jobType,
                                           int limit) {
        Specification<SalesVideoJob> spec = Specification.where(null);
        if (providerFamily != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("providerFamily"), providerFamily));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (jobType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("jobType"), jobType));
        }
        Pageable pageable = PageRequest.of(0,
                Math.max(1, Math.min(limit <= 0 ? 50 : limit, MAX_PAGE_SIZE)),
                Sort.by(Sort.Direction.ASC, "requestedAt"));
        return jobRepository.findAll(spec, pageable)
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesVideoJobDto getJob(Long jobId) {
        return SalesVideoMapper.toDto(loadJob(jobId));
    }

    @Transactional(readOnly = true)
    public List<SalesVideoJobEventDto> getJobEvents(Long jobId) {
        ensureJobExists(jobId);
        return eventRepository.findByJobIdOrderByCreatedAtAsc(jobId)
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesVideoJobDto> listJobsByProfile(Long profileId) {
        ensureProfileAccessible(profileId);
        return jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)
                .stream()
                .map(SalesVideoMapper::toDto)
                .toList();
    }

    @Transactional
    public SalesVideoJobDto claimJob(Long jobId, JobClaimRequest request) {
        SalesVideoJob job = loadJob(jobId);
        if (job.getStartedAt() == null) {
            job.setStartedAt(Instant.now());
        }
        jobRepository.save(job);
        registerEvent(job, SalesVideoJobEventType.CLAIMED, job.getStatus(), job.getStatus(),
                "claim por " + request.getWorkerId(), request.getMessage());
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoJobDto heartbeat(Long jobId, JobHeartbeatRequest request) {
        SalesVideoJob job = loadJob(jobId);
        registerEvent(job, SalesVideoJobEventType.HEARTBEAT, job.getStatus(), job.getStatus(),
                request.getMessage(), request.getDetailsJson());
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoJobDto progress(Long jobId, JobProgressRequest request) {
        SalesVideoJob job = loadJob(jobId);
        if (request.getProgressPercent() != null) {
            job.setProgressPercent(Math.max(0, Math.min(100, request.getProgressPercent())));
        }
        SalesVideoStatus oldStatus = job.getStatus();
        if (request.getStatus() != null && request.getStatus() != job.getStatus()) {
            job.setStatus(request.getStatus());
            maybeUpdateProfileStatus(job, request.getStatus());
        }
        jobRepository.save(job);
        registerEvent(job, SalesVideoJobEventType.PROGRESS, oldStatus, job.getStatus(),
                request.getMessage(), request.getDetailsJson());
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoJobDto complete(Long jobId, JobCompletionRequest request) {
        SalesVideoJob job = loadJob(jobId);
        SalesVideoStatus previous = job.getStatus();
        SalesVideoStatus finalStatus = Optional.ofNullable(request.getStatus())
                .orElse(defaultCompletionStatus(job.getJobType()));
        job.setStatus(finalStatus);
        job.setFinishedAt(Instant.now());
        job.setProviderJobId(request.getProviderJobId());
        job.setMetadataJson(request.getMetadataJson());
        attachAsset(job::setAsset, request.getAssetId());
        attachAsset(job::setPosterAsset, request.getPosterAssetId());
        attachAsset(job::setVttAsset, request.getVttAssetId());
        SalesVideoScript persistedScript = maybePersistScriptResult(job, request.getScriptResult());
        if (persistedScript != null) {
            job.setScript(persistedScript);
        }
        jobRepository.save(job);
        maybeUpdateProfileStatus(job, finalStatus);
        registerEvent(job, SalesVideoJobEventType.COMPLETED, previous, finalStatus,
                request.getMessage(), request.getDetailsJson());
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoJobDto fail(Long jobId, JobFailureRequest request) {
        SalesVideoJob job = loadJob(jobId);
        SalesVideoStatus previous = job.getStatus();
        SalesVideoStatus newStatus = Optional.ofNullable(request.getStatus())
                .orElse(SalesVideoStatus.VIDEO_FAILED);
        job.setStatus(newStatus);
        job.setFailureCode(request.getFailureCode());
        job.setFailureDetail(request.getFailureDetail());
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
        maybeUpdateProfileStatus(job, newStatus);
        registerEvent(job, SalesVideoJobEventType.FAILED, previous, newStatus,
                request.getMessage(), request.getFailureDetail());
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoJobDto expire(Long jobId, JobExpirationRequest request) {
        SalesVideoJob job = loadJob(jobId);
        SalesVideoStatus previous = job.getStatus();
        job.setStatus(SalesVideoStatus.VIDEO_FAILED);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
        maybeUpdateProfileStatus(job, SalesVideoStatus.VIDEO_FAILED);
        registerEvent(job, SalesVideoJobEventType.EXPIRED, previous, SalesVideoStatus.VIDEO_FAILED,
                request.getMessage(), request.getDetailsJson());
        return SalesVideoMapper.toDto(job);
    }

    @Transactional
    public SalesVideoJobDto retry(Long jobId, RetrySalesVideoJobRequest request) {
        SalesVideoJob job = loadJob(jobId);
        reprocessPolicy.ensureRetryAllowed(job, request.getReason());
        SalesVideoScript script = job.getScript() == null ? null
                : scriptRepository.findById(job.getScript().getId()).orElse(null);
        String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
        SalesVideoJob newJob = createJob(job.getProfile(),
                script,
                job.getJobType(),
                job.getProviderFamily(),
                job.getProviderName(),
                requestedBy);
        newJob.setRetryOfJob(job);
        newJob.setRetryAttempt(job.getRetryAttempt() + 1);
        newJob.setRetryReason(request.getReason());
        newJob.setRetryNotes(request.getNotes());
        jobRepository.save(newJob);
        registerEvent(job, SalesVideoJobEventType.RETRIED, job.getStatus(), job.getStatus(),
                "Reprocessamento solicitado por " + requestedBy + " (" + request.getReason().name() + ")",
                request.getNotes());
        return SalesVideoMapper.toDto(newJob);
    }

    private SalesVideoScript maybePersistScriptResult(SalesVideoJob job,
                                                          GeneratedScriptResultPayload payload) {
        if (job.getJobType() != SalesVideoJobType.SCRIPT || payload == null) {
            return null;
        }
        if (!StringUtils.hasText(payload.getScriptText()) || job.getProfile() == null) {
            return null;
        }
        int nextVersion = scriptRepository.findFirstByProfileIdOrderByVersionDesc(job.getProfile().getId())
                .map(SalesVideoScript::getVersion)
                .map(v -> v + 1)
                .orElse(1);
        String createdBy = StringUtils.hasText(job.getRequestedBy()) ? job.getRequestedBy() : "system@marketinghub.io";
        SalesVideoScript script = SalesVideoScript.builder()
                .profile(job.getProfile())
                .version(nextVersion)
                .createdBy(createdBy)
                .scriptText(payload.getScriptText())
                .hookText(payload.getHookText())
                .ctaText(payload.getCtaText())
                .captionText(payload.getCaptionText())
                .storyboardJson(payload.getStoryboardJson())
                .source(SalesVideoScriptSource.OPENAI)
                .model(payload.getModel())
                .prompt(payload.getPrompt())
                .status(SalesVideoScriptStatus.READY_FOR_REVIEW)
                .build();
        SalesVideoScript saved = scriptRepository.save(script);
        if (job.getProfile().getScripts() != null) {
            job.getProfile().getScripts().add(saved);
        }
        return saved;
    }

    private void attachAsset(java.util.function.Consumer<Asset> setter, Long assetId) {
        if (assetId == null) {
            setter.accept(null);
            return;
        }
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> VideoModuleException.badRequest(VideoModuleErrorCode.ASSET_NOT_FOUND,
                        "Asset não encontrado: " + assetId));
        setter.accept(asset);
    }

    private SalesVideoJob loadJob(Long jobId) {
        SalesVideoJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.JOB_NOT_FOUND,
                        "Job não encontrado: " + jobId));
        TenantContextHolder.assertTenant(job.getTenantId());
        return job;
    }

    private void ensureJobExists(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw VideoModuleException.notFound(VideoModuleErrorCode.JOB_NOT_FOUND,
                    "Job não encontrado: " + jobId);
        }
    }

    private void ensureProfileAccessible(Long profileId) {
        SalesVideoProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Perfil de vídeo não encontrado: " + profileId));
        TenantContextHolder.assertTenant(profile.getTenantId());
    }

    private SalesVideoStatus initialStatus(SalesVideoJobType jobType) {
        return switch (jobType) {
            case SCRIPT -> SalesVideoStatus.SCRIPT_PENDING;
            case STORYBOARD -> SalesVideoStatus.STORYBOARD_PENDING;
            case RENDER, RETRY -> SalesVideoStatus.VIDEO_REQUESTED;
            case PUBLISH -> SalesVideoStatus.PUBLISHED;
        };
    }

    private SalesVideoStatus defaultCompletionStatus(SalesVideoJobType jobType) {
        return switch (jobType) {
            case SCRIPT -> SalesVideoStatus.SCRIPT_READY;
            case STORYBOARD -> SalesVideoStatus.STORYBOARD_READY;
            case RENDER, RETRY -> SalesVideoStatus.VIDEO_READY;
            case PUBLISH -> SalesVideoStatus.PUBLISHED;
        };
    }

    private void registerEvent(SalesVideoJob job,
                               SalesVideoJobEventType type,
                               SalesVideoStatus oldStatus,
                               SalesVideoStatus newStatus,
                               String message,
                               String detailsJson) {
        SalesVideoJobEvent event = SalesVideoJobEvent.builder()
                .job(job)
                .eventType(type)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .message(message)
                .detailsJson(detailsJson)
                .build();
        eventRepository.save(event);
        job.getEvents().add(event);
    }

    private void maybeUpdateProfileStatus(SalesVideoJob job, SalesVideoStatus status) {
        if (status == null || job.getProfile() == null) {
            return;
        }
        EnumSet<SalesVideoStatus> syncable = EnumSet.of(
                SalesVideoStatus.SCRIPT_PENDING,
                SalesVideoStatus.SCRIPT_READY,
                SalesVideoStatus.STORYBOARD_PENDING,
                SalesVideoStatus.STORYBOARD_READY,
                SalesVideoStatus.VIDEO_REQUESTED,
                SalesVideoStatus.VIDEO_PROCESSING,
                SalesVideoStatus.VIDEO_READY,
                SalesVideoStatus.VIDEO_FAILED,
                SalesVideoStatus.PUBLISHED);
        if (syncable.contains(status)) {
            job.getProfile().setStatus(status);
            profileRepository.save(job.getProfile());
        }
    }
}
