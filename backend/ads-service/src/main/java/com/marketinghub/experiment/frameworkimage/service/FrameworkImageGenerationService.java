package com.marketinghub.experiment.frameworkimage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gerencia a fila de geração de imagens da landing e materializa o manifesto consolidado de assets do experimento.
 */
@Service
public class FrameworkImageGenerationService {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageGenerationService.class);
    private final FrameworkImageGenerationJobRepository jobRepository;
    private final ExperimentRepository experimentRepository;
    private final ObjectMapper objectMapper;
    private final boolean rolloutEnabled;
    private final int rolloutPercentage;

    /** Inicializa o serviço com repositórios, mapper JSON e configuração de rollout da geração de imagens. */
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

    /** Finaliza um job de imagem e atualiza o manifesto consolidado de assets do experimento. */
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
        refreshLandingPageImageAssets(job.getExperiment());
    }

    /** Marca um job de imagem como falho e atualiza o manifesto consolidado com o erro operacional. */
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
        refreshLandingPageImageAssets(job.getExperiment());
    }

    /** Registra a URL web definitiva de um asset e atualiza o manifesto consolidado do experimento. */
    @Transactional
    public void markAssetAsWebReady(Long assetId, String webUrl) {
        FrameworkImageGenerationJob job = jobRepository.findFirstByAssetIdOrderByCreatedAtDesc(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset não encontrado"));
        String normalizedWebUrl = normalizeRequired(webUrl, "webUrl é obrigatório");

        if (normalizedWebUrl.equals(job.getWebUrl()) && job.getStage() == FrameworkImageGenerationJobStage.WEB_READY) {
            refreshLandingPageImageAssets(job.getExperiment());
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
        refreshLandingPageImageAssets(job.getExperiment());
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
                            null,
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

    /** Recria e persiste o manifesto consolidado de URLs de imagens da landing para consumo das etapas seguintes. */
    private void refreshLandingPageImageAssets(Experiment experiment) {
        if (experiment == null || experiment.getId() == null) {
            return;
        }
        List<PlanningItem> planningItems = parsePlanningItems(experiment);
        List<FrameworkImageGenerationJob> jobs = jobRepository.findByExperimentIdOrderByCreatedAtDesc(experiment.getId());
        String manifest = buildLandingPageImageAssetsManifest(experiment.getId(), planningItems, jobs);
        experiment.setLandingPageImageAssets(manifest);
        experimentRepository.save(experiment);
    }

    /** Monta o JSON versionado com o último job conhecido de cada item planejado de imagem. */
    private String buildLandingPageImageAssetsManifest(Long experimentId,
                                                       List<PlanningItem> planningItems,
                                                       List<FrameworkImageGenerationJob> jobs) {
        Map<String, FrameworkImageGenerationJob> latestByItem = jobs.stream()
                .filter(job -> job != null && StringUtils.hasText(job.getPlanningItemKey()))
                .collect(Collectors.toMap(
                        FrameworkImageGenerationJob::getPlanningItemKey,
                        job -> job,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        ObjectNode root = objectMapper.createObjectNode();
        root.put("version", 1);
        root.put("experimentId", experimentId);
        root.put("generatedAt", Instant.now().toString());
        ArrayNode images = root.putArray("images");

        for (PlanningItem item : planningItems) {
            FrameworkImageGenerationJob job = latestByItem.remove(item.planningItemKey());
            images.add(toManifestImageNode(item, job));
        }
        for (FrameworkImageGenerationJob job : latestByItem.values()) {
            images.add(toManifestImageNode(new PlanningItem(job.getPlanningItemKey(), null, null, null, null), job));
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            log.error("Erro ao serializar manifesto de imagens da landing (experimentId={}, imageCount={})",
                    experimentId,
                    images.size(),
                    ex);
            throw new IllegalStateException("Falha ao serializar manifesto de imagens da landing", ex);
        }
    }

    /** Converte item planejado e último job em um nó do manifesto consolidado de imagens. */
    private ObjectNode toManifestImageNode(PlanningItem item, FrameworkImageGenerationJob job) {
        ObjectNode image = objectMapper.createObjectNode();
        image.put("planningItemKey", item.planningItemKey());
        putIfPresent(image, "sectionId", item.sectionId());
        putIfPresent(image, "sectionName", item.sectionName());
        putIfPresent(image, "elementId", item.elementId());
        putIfPresent(image, "prompt", item.prompt());
        if (job != null) {
            if (job.getId() != null) {
                image.put("jobId", job.getId().toString());
            }
            if (job.getStatus() != null) {
                image.put("status", job.getStatus().name());
            }
            if (job.getStage() != null) {
                image.put("stage", job.getStage().name());
            }
            putIfPresent(image, "model", job.getModel());
            if (job.getAssetId() != null) {
                image.put("assetId", job.getAssetId());
            }
            putIfPresent(image, "sourceUrl", job.getSourceUrl());
            putIfPresent(image, "webUrl", job.getWebUrl());
            putIfPresent(image, "resolvedUrl", firstNonBlank(job.getWebUrl(), job.getSourceUrl()));
            putIfPresent(image, "errorMessage", job.getErrorMessage());
            putIfPresent(image, "createdAt", instantToString(job.getCreatedAt()));
            putIfPresent(image, "startedAt", instantToString(job.getStartedAt()));
            putIfPresent(image, "finishedAt", instantToString(job.getFinishedAt()));
        } else {
            image.put("status", "PLANNED");
        }
        return image;
    }

    /** Adiciona um campo textual ao JSON somente quando houver valor útil. */
    private void putIfPresent(ObjectNode node, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            node.put(fieldName, value.trim());
        }
    }

    /** Converte Instant para texto ISO-8601 mantendo nulo quando não há data. */
    private String instantToString(Instant value) {
        return value != null ? value.toString() : null;
    }

    /** Retorna o primeiro texto preenchido entre os candidatos informados. */
    private String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return null;
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
        JsonNode rootNode = parsePlanningJsonNode(experiment.getLandingPageImagePlanning());
        if (rootNode == null) {
            return List.of();
        }
        try {
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
                String sectionId = normalize(imageNode.path("sectionId").asText(null));
                String planningItemKey = normalizePlanningItemKey(sectionId, index);
                items.add(new PlanningItem(
                        planningItemKey,
                        sectionId,
                        normalize(imageNode.path("sectionName").asText(null)),
                        normalize(imageNode.path("elementId").asText(null)),
                        normalize(promptFrom(imageNode))));
            }
            return deduplicatePlanningItems(items);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private JsonNode parsePlanningJsonNode(String rawPlanning) {
        if (!StringUtils.hasText(rawPlanning)) {
            return null;
        }
        JsonNode parsed = readJsonNode(rawPlanning);
        if (parsed != null) {
            return parsed;
        }
        String normalizedPlanning = sanitizeCodeFence(rawPlanning);
        if (!Objects.equals(normalizedPlanning, rawPlanning)) {
            parsed = readJsonNode(normalizedPlanning);
            if (parsed != null) {
                return parsed;
            }
        }
        String unescapedPlanning = unescapeJsonLikeContent(normalizedPlanning);
        if (!Objects.equals(unescapedPlanning, normalizedPlanning)) {
            parsed = readJsonNode(unescapedPlanning);
            if (parsed != null) {
                return parsed;
            }
        }
        String extractedJsonObject = extractFirstJsonObject(unescapedPlanning);
        if (extractedJsonObject != null) {
            return readJsonNode(extractedJsonObject);
        }
        return null;
    }

    private JsonNode readJsonNode(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String sanitizeCodeFence(String rawPlanning) {
        String trimmedPlanning = rawPlanning.trim();
        if (!trimmedPlanning.startsWith("```")) {
            return rawPlanning;
        }
        int firstLineBreak = trimmedPlanning.indexOf('\n');
        if (firstLineBreak < 0) {
            return rawPlanning;
        }
        String maybeBody = trimmedPlanning.substring(firstLineBreak + 1);
        int lastFence = maybeBody.lastIndexOf("```");
        if (lastFence < 0) {
            return rawPlanning;
        }
        return maybeBody.substring(0, lastFence).trim();
    }

    private String extractFirstJsonObject(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int firstBrace = value.indexOf('{');
        if (firstBrace < 0) {
            return null;
        }

        boolean inString = false;
        boolean escaping = false;
        int depth = 0;
        for (int index = firstBrace; index < value.length(); index++) {
            char current = value.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                    continue;
                }
                if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return value.substring(firstBrace, index + 1);
                }
            }
        }
        return null;
    }

    private String unescapeJsonLikeContent(String value) {
        if (!StringUtils.hasText(value) || value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder output = new StringBuilder(value.length());
        boolean escaping = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!escaping) {
                if (current == '\\') {
                    escaping = true;
                } else {
                    output.append(current);
                }
                continue;
            }

            escaping = false;
            switch (current) {
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                case 't' -> output.append('\t');
                case 'b' -> output.append('\b');
                case 'f' -> output.append('\f');
                case '"' -> output.append('"');
                case '\\' -> output.append('\\');
                case '/' -> output.append('/');
                case 'u' -> {
                    if (index + 4 < value.length()) {
                        String hex = value.substring(index + 1, index + 5);
                        try {
                            output.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                            break;
                        } catch (NumberFormatException ignored) {
                            output.append("\\u").append(hex);
                            index += 4;
                            break;
                        }
                    }
                    output.append("\\u");
                }
                default -> output.append(current);
            }
        }
        if (escaping) {
            output.append('\\');
        }
        return output.toString();
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
        if (imagePlan.isArray()) {
            return objectMapper.createObjectNode().set("images", imagePlan);
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
                    item.sectionId(),
                    item.sectionName(),
                    item.elementId(),
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

    /** Representa um item planejado de imagem normalizado para filas e manifesto consolidado. */
    private record PlanningItem(String planningItemKey, String sectionId, String sectionName, String elementId, String prompt) {
        private PlanningItem {
            Objects.requireNonNull(planningItemKey);
        }
    }
}
