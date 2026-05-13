package com.marketinghub.experiment.frameworkimage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationItemStatusDto;
import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationSummaryDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobCompletionRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageWebnizationPendingAssetDto;
import com.marketinghub.experiment.frameworkimage.repository.FrameworkImageGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FrameworkImageGenerationService {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageGenerationService.class);
    private final FrameworkImageGenerationJobRepository jobRepository;
    private final ExperimentRepository experimentRepository;
    private final ObjectMapper objectMapper;
    private final boolean rolloutEnabled;
    private final int rolloutPercentage;

    public FrameworkImageGenerationService(FrameworkImageGenerationJobRepository jobRepository,
                                           ExperimentRepository experimentRepository,
                                           ObjectMapper objectMapper,
                                           @Value("${framework-image.rollout.enabled:true}") boolean rolloutEnabled,
                                           @Value("${framework-image.rollout.percentage:100}") int rolloutPercentage) {
        this.jobRepository = jobRepository;
        this.experimentRepository = experimentRepository;
        this.objectMapper = objectMapper;
        this.rolloutEnabled = rolloutEnabled;
        this.rolloutPercentage = Math.min(100, Math.max(0, rolloutPercentage));
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

    @Transactional(readOnly = true)
    public List<FrameworkImageWebnizationPendingAssetDto> listPendingWebnizationAssets(int limit) {
        return jobRepository.findByStatusAndStageInAndAssetIdIsNotNullAndSourceUrlIsNotNullAndWebUrlIsNullOrderByUpdatedAtAsc(
                        FrameworkImageGenerationJobStatus.COMPLETED,
                        Set.of(FrameworkImageGenerationJobStage.NOTIFIED_BACKEND, FrameworkImageGenerationJobStage.WAITING_WEBNIZATION),
                        PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(job -> new FrameworkImageWebnizationPendingAssetDto(
                        job.getId(),
                        job.getExperiment().getId(),
                        job.getPlanningItemKey(),
                        job.getAssetId(),
                        job.getSourceUrl(),
                        job.getUpdatedAt()))
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
        log.info("framework-image-job-claimed jobId={} experimentId={} stage={} workerId={}",
                job.getId(), job.getExperiment().getId(), job.getStage(), job.getWorkerId());
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
        log.info("framework-image-job-completed jobId={} experimentId={} stage={} workerId={} assetId={} batchId={} model={}",
                job.getId(), job.getExperiment().getId(), job.getStage(), job.getWorkerId(),
                job.getAssetId(), job.getBatchId(), job.getModel());
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
        log.warn("framework-image-job-failed jobId={} experimentId={} stage={} workerId={} assetId={} batchId={} error={}",
                job.getId(), job.getExperiment().getId(), job.getStage(), job.getWorkerId(),
                job.getAssetId(), job.getBatchId(), job.getErrorMessage());
    }

    @Transactional
    public void markAssetAsWebReady(Long assetId, String webUrl) {
        FrameworkImageGenerationJob job = jobRepository.findFirstByAssetIdOrderByCreatedAtDesc(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset não encontrado"));
        String normalizedWebUrl = normalizeRequired(webUrl, "webUrl é obrigatório");

        if (normalizedWebUrl.equals(job.getWebUrl()) && job.getStage() == FrameworkImageGenerationJobStage.WEB_READY) {
            return;
        }

        job.setWebUrl(normalizedWebUrl);
        job.setStage(FrameworkImageGenerationJobStage.WEB_READY);
        if (job.getStatus() != FrameworkImageGenerationJobStatus.COMPLETED) {
            job.setStatus(FrameworkImageGenerationJobStatus.COMPLETED);
        }
        if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }
        job.setErrorMessage(null);
        log.info("framework-image-asset-web-ready jobId={} experimentId={} stage={} assetId={} webUrl={}",
                job.getId(), job.getExperiment().getId(), job.getStage(), job.getAssetId(), job.getWebUrl());
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

    @Transactional
    public List<FrameworkImageGenerationJobDto> enqueueJobsForExperiment(Long experimentId) {
        if (!isRolloutEligible(experimentId)) {
            log.info("framework-image-rollout-skipped experimentId={} enabled={} percentage={}",
                    experimentId, rolloutEnabled, rolloutPercentage);
            return List.of();
        }

        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));

        return parsePlanningItems(experiment).stream()
                .filter(item -> StringUtils.hasText(item.prompt()))
                .map(item -> enqueueJob(experimentId, item.planningItemKey(), null, item.prompt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean allPlanningImagesCompleted(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
        List<PlanningItem> items = parsePlanningItems(experiment).stream()
                .filter(item -> StringUtils.hasText(item.prompt()))
                .toList();
        if (items.isEmpty()) {
            return true;
        }
        Map<String, FrameworkImageGenerationJob> latestByItem = new LinkedHashMap<>();
        for (FrameworkImageGenerationJob job : jobRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId)) {
            latestByItem.putIfAbsent(job.getPlanningItemKey(), job);
        }
        for (PlanningItem item : items) {
            FrameworkImageGenerationJob latest = latestByItem.get(item.planningItemKey());
            if (latest == null || latest.getStatus() != FrameworkImageGenerationJobStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public int failStaleProcessingJobs(Instant staleBefore, int limit, String staleReason) {
        List<FrameworkImageGenerationJob> staleJobs = jobRepository.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                FrameworkImageGenerationJobStatus.PROCESSING,
                staleBefore,
                PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
        for (FrameworkImageGenerationJob job : staleJobs) {
            job.setStatus(FrameworkImageGenerationJobStatus.FAILED);
            job.setStage(FrameworkImageGenerationJobStage.FAILED);
            job.setFinishedAt(Instant.now());
            job.setErrorMessage(staleReason);
            log.warn("framework-image-job-stale-timeout jobId={} experimentId={} stage={} workerId={} assetId={} batchId={} startedAt={}",
                    job.getId(), job.getExperiment().getId(), job.getStage(), job.getWorkerId(),
                    job.getAssetId(), job.getBatchId(), job.getStartedAt());
        }
        return staleJobs.size();
    }

    @Transactional(readOnly = true)
    public List<FrameworkImageGenerationItemStatusDto> listJobsByExperiment(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));

        Map<String, PlanningItem> planningItems = new LinkedHashMap<>();
        for (PlanningItem item : parsePlanningItems(experiment)) {
            planningItems.put(item.planningItemKey(), item);
        }

        Map<String, FrameworkImageGenerationJob> latestJobByPlanningItem = new LinkedHashMap<>();
        for (FrameworkImageGenerationJob job : jobRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId)) {
            latestJobByPlanningItem.putIfAbsent(job.getPlanningItemKey(), job);
        }

        List<FrameworkImageGenerationItemStatusDto> response = new ArrayList<>();
        for (PlanningItem item : planningItems.values()) {
            FrameworkImageGenerationJob job = latestJobByPlanningItem.remove(item.planningItemKey());
            if (job == null) {
                response.add(new FrameworkImageGenerationItemStatusDto(
                        item.planningItemKey(),
                        item.sectionName(),
                        item.prompt(),
                        null,
                        "PLANNED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
                continue;
            }
            response.add(toItemStatusDto(item, job));
        }

        for (FrameworkImageGenerationJob remainingJob : latestJobByPlanningItem.values()) {
            response.add(toItemStatusDto(new PlanningItem(
                            remainingJob.getPlanningItemKey(),
                            null,
                            remainingJob.getPrompt()),
                    remainingJob));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public FrameworkImageGenerationSummaryDto summarizeJobsByExperiment(Long experimentId) {
        List<FrameworkImageGenerationItemStatusDto> items = listJobsByExperiment(experimentId);
        int planned = 0;
        int processing = 0;
        int waitingOpenAiBatch = 0;
        int completed = 0;
        int failed = 0;
        Instant updatedAt = null;

        for (FrameworkImageGenerationItemStatusDto item : items) {
            String status = item.status() == null ? "" : item.status().trim().toUpperCase();
            String stage = item.stage() == null ? "" : item.stage().trim().toUpperCase();
            if ("PLANNED".equals(status) || "PENDING".equals(status)) {
                planned++;
            }
            if ("PROCESSING".equals(status)) {
                processing++;
            }
            if ("WAITING_OPENAI_BATCH".equals(stage)) {
                waitingOpenAiBatch++;
            }
            if ("COMPLETED".equals(status)) {
                completed++;
            }
            if ("FAILED".equals(status)) {
                failed++;
            }
            if (item.updatedAt() != null && (updatedAt == null || item.updatedAt().isAfter(updatedAt))) {
                updatedAt = item.updatedAt();
            }
        }

        return new FrameworkImageGenerationSummaryDto(
                items.size(),
                planned,
                processing,
                waitingOpenAiBatch,
                completed,
                failed,
                updatedAt);
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

    private boolean isRolloutEligible(Long experimentId) {
        if (!rolloutEnabled || rolloutPercentage <= 0) {
            return false;
        }
        if (rolloutPercentage >= 100) {
            return true;
        }
        long normalizedExperimentId = experimentId == null ? 0L : Math.abs(experimentId);
        long bucket = normalizedExperimentId % 100;
        return bucket < rolloutPercentage;
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

    private FrameworkImageGenerationItemStatusDto toItemStatusDto(PlanningItem item, FrameworkImageGenerationJob job) {
        return new FrameworkImageGenerationItemStatusDto(
                item.planningItemKey(),
                item.sectionName(),
                item.prompt(),
                job.getId(),
                job.getStatus().name(),
                job.getStage().name(),
                job.getModel(),
                job.getAssetId(),
                job.getSourceUrl(),
                job.getWebUrl(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    private List<PlanningItem> parsePlanningItems(Experiment experiment) {
        String rawPlanning = experiment.getLandingPageImagePlanning();
        if (!StringUtils.hasText(rawPlanning)) {
            return List.of();
        }
        try {
            JsonNode rootNode = objectMapper.readTree(rawPlanning);
            JsonNode planningNode = resolvePlanningNode(rootNode);
            JsonNode imagesNode = planningNode.path("images");
            if (!imagesNode.isArray()) {
                return List.of();
            }

            List<PlanningItem> items = new ArrayList<>();
            for (int index = 0; index < imagesNode.size(); index++) {
                JsonNode imageNode = imagesNode.get(index);
                if (imageNode == null || !imageNode.isObject()) {
                    continue;
                }
                String planningItemKey = normalizePlanningItemKey(imageNode.path("sectionId").asText(null), index);
                items.add(new PlanningItem(
                        planningItemKey,
                        normalize(imageNode.path("sectionName").asText(null)),
                        normalize(promptFrom(imageNode))));
            }
            return deduplicatePlanningItems(items);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private JsonNode resolvePlanningNode(JsonNode rootNode) {
        if (rootNode == null || !rootNode.isObject()) {
            return objectMapper.createObjectNode();
        }

        JsonNode directPlanning = rootNode.path("landingPageImagePlanning");
        if (directPlanning.isObject()) {
            return directPlanning;
        }
        JsonNode imagePlan = rootNode.path("imagePlan");
        if (imagePlan.isObject()) {
            return imagePlan;
        }
        JsonNode artifactContent = rootNode.path("artifact").path("content");
        if (artifactContent.isObject()) {
            return artifactContent;
        }
        return rootNode;
    }

    private String promptFrom(JsonNode imageNode) {
        String imagePrompt = normalize(imageNode.path("imagePrompt").asText(null));
        if (imagePrompt != null) {
            return imagePrompt;
        }
        return normalize(imageNode.path("prompt").asText(null));
    }

    private List<PlanningItem> deduplicatePlanningItems(List<PlanningItem> items) {
        Map<String, Integer> duplicates = new LinkedHashMap<>();
        List<PlanningItem> normalizedItems = new ArrayList<>(items.size());
        for (PlanningItem item : items) {
            int occurrence = duplicates.compute(item.planningItemKey(), (key, count) -> count == null ? 1 : count + 1);
            if (occurrence == 1) {
                normalizedItems.add(item);
                continue;
            }
            normalizedItems.add(new PlanningItem(
                    item.planningItemKey() + "-" + occurrence,
                    item.sectionName(),
                    item.prompt()));
        }
        return normalizedItems;
    }

    private String normalizePlanningItemKey(String value, int index) {
        String normalized = normalize(value);
        if (normalized != null) {
            return normalized;
        }
        return "item-" + (index + 1);
    }

    private record PlanningItem(String planningItemKey, String sectionName, String prompt) {
        private PlanningItem {
            Objects.requireNonNull(planningItemKey);
        }
    }
}
