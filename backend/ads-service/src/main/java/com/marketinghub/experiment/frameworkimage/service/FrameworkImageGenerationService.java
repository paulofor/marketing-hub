package com.marketinghub.experiment.frameworkimage.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobCompletionRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.repository.FrameworkImageGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FrameworkImageGenerationService {
    private final FrameworkImageGenerationJobRepository jobRepository;
    private final ExperimentRepository experimentRepository;

    public FrameworkImageGenerationService(FrameworkImageGenerationJobRepository jobRepository,
                                           ExperimentRepository experimentRepository) {
        this.jobRepository = jobRepository;
        this.experimentRepository = experimentRepository;
    }

    @Transactional(readOnly = true)
    public List<FrameworkImageGenerationJobDto> listPendingJobs(int limit) {
        return jobRepository.findByStatusOrderByCreatedAtAsc(
                        FrameworkImageGenerationJobStatus.PENDING,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FrameworkImageGenerationJobDto claimJob(UUID jobId, String workerId) {
        FrameworkImageGenerationJob job = findJob(jobId);
        if (job.getStatus() != FrameworkImageGenerationJobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job não está pendente");
        }
        job.setStatus(FrameworkImageGenerationJobStatus.PROCESSING);
        job.setStage(FrameworkImageGenerationJobStage.CLAIMED);
        job.setWorkerId(StringUtils.hasText(workerId) ? workerId.trim() : "unknown-worker");
        job.setStartedAt(Instant.now());
        return toDto(job);
    }

    @Transactional
    public void updateJobStage(UUID jobId, FrameworkImageGenerationJobStage stage) {
        FrameworkImageGenerationJob job = findJob(jobId);
        if (job.getStatus() == FrameworkImageGenerationJobStatus.COMPLETED
                || job.getStatus() == FrameworkImageGenerationJobStatus.FAILED) {
            return;
        }
        job.setStage(stage != null ? stage : job.getStage());
    }

    @Transactional
    public void completeJob(UUID jobId, FrameworkImageGenerationJobCompletionRequest request) {
        FrameworkImageGenerationJob job = findJob(jobId);
        if (job.getStatus() == FrameworkImageGenerationJobStatus.COMPLETED) {
            return;
        }
        if (job.getStatus() != FrameworkImageGenerationJobStatus.PROCESSING
                && job.getStatus() != FrameworkImageGenerationJobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job não pode ser finalizado");
        }

        job.setStatus(FrameworkImageGenerationJobStatus.COMPLETED);
        job.setStage(request.stage() != null ? request.stage() : FrameworkImageGenerationJobStage.NOTIFIED_BACKEND);
        job.setModel(normalize(request.model()));
        job.setPrompt(normalize(request.prompt()));
        job.setBatchId(normalize(request.batchId()));
        job.setAssetId(request.assetId());
        job.setSourceUrl(normalize(request.sourceUrl()));
        job.setWebUrl(normalize(request.webUrl()));
        job.setErrorMessage(null);
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        FrameworkImageGenerationJob job = findJob(jobId);
        if (job.getStatus() == FrameworkImageGenerationJobStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job já finalizado");
        }

        job.setStatus(FrameworkImageGenerationJobStatus.FAILED);
        job.setStage(FrameworkImageGenerationJobStage.FAILED);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Falha desconhecida");
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public FrameworkImageGenerationJobDto enqueueJob(Long experimentId,
                                                     String planningItemKey,
                                                     String model,
                                                     String prompt) {
        String normalizedPlanningItemKey = normalizeRequired(planningItemKey, "planningItemKey é obrigatório");

        FrameworkImageGenerationJob existing = jobRepository
                .findFirstByExperimentIdAndPlanningItemKeyAndStatusInOrderByCreatedAtDesc(
                        experimentId,
                        normalizedPlanningItemKey,
                        Set.of(FrameworkImageGenerationJobStatus.PENDING, FrameworkImageGenerationJobStatus.PROCESSING))
                .orElse(null);
        if (existing != null) {
            return toDto(existing);
        }

        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));

        FrameworkImageGenerationJob saved = jobRepository.save(FrameworkImageGenerationJob.builder()
                .experiment(experiment)
                .planningItemKey(normalizedPlanningItemKey)
                .model(normalize(model))
                .prompt(normalize(prompt))
                .status(FrameworkImageGenerationJobStatus.PENDING)
                .stage(FrameworkImageGenerationJobStage.WAITING_AI_WORKER)
                .build());
        return toDto(saved);
    }

    private FrameworkImageGenerationJob findJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private FrameworkImageGenerationJobDto toDto(FrameworkImageGenerationJob job) {
        return FrameworkImageGenerationJobDto.builder()
                .id(job.getId())
                .experimentId(job.getExperiment().getId())
                .planningItemKey(job.getPlanningItemKey())
                .status(job.getStatus().name())
                .stage(job.getStage().name())
                .workerId(job.getWorkerId())
                .model(job.getModel())
                .prompt(job.getPrompt())
                .batchId(job.getBatchId())
                .assetId(job.getAssetId())
                .sourceUrl(job.getSourceUrl())
                .webUrl(job.getWebUrl())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
