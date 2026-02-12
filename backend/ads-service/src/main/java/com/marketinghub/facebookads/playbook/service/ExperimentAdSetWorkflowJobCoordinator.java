package com.marketinghub.facebookads.playbook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJob;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobApiLog;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobStatus;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobType;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpec;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpecSlot;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflowStatus;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobApiLogRequest;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobApiLogRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetSpecRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetWorkflowRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Coordinates job scheduling and orchestration for the ad set playbook.
 */
@Service
public class ExperimentAdSetWorkflowJobCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentAdSetWorkflowJobCoordinator.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final String DEFAULT_COUNTRY = "BR";
    private static final String DEFAULT_LOCALE = "pt_BR";
    private static final String DEFAULT_PROVIDER = "FACEBOOK";
    private static final int SEED_SEARCH_LIMIT = 25;
    private static final int SUGGESTION_LIMIT = 100;
    private static final int POSITION_LIMIT = 25;

    private final ExperimentAdSetJobRepository jobRepository;
    private final ExperimentAdSetJobApiLogRepository jobApiLogRepository;
    private final ExperimentAdSetWorkflowRepository workflowRepository;
    private final ExperimentAdSetSpecRepository specRepository;
    private final ExperimentRepository experimentRepository;
    private final FacebookAccountRepository facebookAccountRepository;
    private final ObjectMapper objectMapper;

    public ExperimentAdSetWorkflowJobCoordinator(ExperimentAdSetJobRepository jobRepository,
                                                 ExperimentAdSetJobApiLogRepository jobApiLogRepository,
                                                 ExperimentAdSetWorkflowRepository workflowRepository,
                                                 ExperimentAdSetSpecRepository specRepository,
                                                 ExperimentRepository experimentRepository,
                                                 FacebookAccountRepository facebookAccountRepository,
                                                 ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobApiLogRepository = jobApiLogRepository;
        this.workflowRepository = workflowRepository;
        this.specRepository = specRepository;
        this.experimentRepository = experimentRepository;
        this.facebookAccountRepository = facebookAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void initializeWorkflow(ExperimentAdSetWorkflow workflow) {
        workflow.setStatus(ExperimentAdSetWorkflowStatus.RUNNING);
        workflow.setLastError(null);
        workflowRepository.save(workflow);
        enqueueSeedPlanningJob(workflow);
    }

    @Transactional
    public List<ExperimentAdSetJob> claimJobs(ExperimentAdSetWorker worker, String workerId, int limit) {
        releaseExpiredLocks();
        List<ExperimentAdSetJob> candidates = jobRepository
                .findTop50ByWorkerAndStatusOrderByCreatedAtAsc(worker, ExperimentAdSetJobStatus.PENDING);
        if (CollectionUtils.isEmpty(candidates)) {
            return List.of();
        }
        List<ExperimentAdSetJob> claimed = new ArrayList<>();
        Instant now = Instant.now();
        for (ExperimentAdSetJob pending : candidates) {
            if (jobRepository.claimJob(pending.getId(), workerId, now,
                    ExperimentAdSetJobStatus.PENDING, ExperimentAdSetJobStatus.RUNNING) == 1) {
                jobRepository.findById(pending.getId()).ifPresent(claimed::add);
            }
            if (claimed.size() >= limit) {
                break;
            }
        }
        return claimed;
    }

    @Transactional
    public ExperimentAdSetJob completeJob(Long jobId, JsonNode result,
                                            List<ExperimentAdSetJobApiLogRequest> apiCalls) {
        ExperimentAdSetJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job %d não encontrado".formatted(jobId)));
        if (job.getStatus() != ExperimentAdSetJobStatus.RUNNING) {
            throw new IllegalStateException("Job %d não está em processamento".formatted(jobId));
        }
        job.setStatus(ExperimentAdSetJobStatus.SUCCEEDED);
        job.setResultPayload(result != null ? writeJson(result) : null);
        job.setFinishedAt(Instant.now());
        job.setLockedAt(null);
        job.setLockedBy(null);
        jobRepository.save(job);
        replaceApiLogs(job, apiCalls);
        handleJobSuccess(job);
        return job;
    }

    @Transactional
    public ExperimentAdSetJob failJob(Long jobId, String errorMessage,
                                           List<ExperimentAdSetJobApiLogRequest> apiCalls) {
        ExperimentAdSetJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job %d não encontrado".formatted(jobId)));
        job.setStatus(ExperimentAdSetJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setFinishedAt(Instant.now());
        job.setLockedAt(null);
        job.setLockedBy(null);
        jobRepository.save(job);
        replaceApiLogs(job, apiCalls);
        handleJobFailure(job, errorMessage);
        return job;
    }

    private void replaceApiLogs(ExperimentAdSetJob job, List<ExperimentAdSetJobApiLogRequest> apiCalls) {
        if (job == null || job.getId() == null) {
            return;
        }
        jobApiLogRepository.deleteByJobId(job.getId());
        if (CollectionUtils.isEmpty(apiCalls)) {
            return;
        }
        List<ExperimentAdSetJobApiLog> logs = new ArrayList<>();
        for (ExperimentAdSetJobApiLogRequest call : apiCalls) {
            if (call == null) {
                continue;
            }
            ExperimentAdSetJobApiLog log = ExperimentAdSetJobApiLog.builder()
                    .job(job)
                    .provider(StringUtils.hasText(call.provider()) ? call.provider().trim() : DEFAULT_PROVIDER)
                    .endpoint(call.endpoint())
                    .httpMethod(call.httpMethod())
                    .statusCode(call.statusCode())
                    .requestedAt(call.requestedAt())
                    .respondedAt(call.respondedAt())
                    .requestPayload(call.requestPayload() != null ? writeJson(call.requestPayload()) : null)
                    .responsePayload(call.responsePayload() != null ? writeJson(call.responsePayload()) : null)
                    .errorMessage(call.errorMessage())
                    .build();
            logs.add(log);
        }
        if (!logs.isEmpty()) {
            jobApiLogRepository.saveAll(logs);
        }
    }

    private void releaseExpiredLocks() {
        Instant threshold = Instant.now().minus(LOCK_TTL);
        int released = jobRepository.releaseExpiredLocks(threshold,
                ExperimentAdSetJobStatus.RUNNING,
                ExperimentAdSetJobStatus.PENDING);
        if (released > 0) {
            LOGGER.warn("Liberados {} jobs travados na fila do playbook", released);
        }
    }

    private void enqueueSeedPlanningJob(ExperimentAdSetWorkflow workflow) {
        if (jobRepository.existsByWorkflowIdAndTypeAndStatusIn(
                workflow.getId(),
                ExperimentAdSetJobType.AI_PREPARE_SEED,
                EnumSet.of(ExperimentAdSetJobStatus.PENDING, ExperimentAdSetJobStatus.RUNNING))) {
            return;
        }
        Experiment experiment = loadExperiment(workflow);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("experimentId", experiment.getId());
        payload.put("experimentName", experiment.getName());
        payload.put("experimentHypothesis", experiment.getHypothesis());
        payload.put("nicheName", experiment.getNiche() != null ? experiment.getNiche().getName() : null);
        payload.put("nicheSegmentation", experiment.getNiche() != null ? experiment.getNiche().getBaseSegmentation() : null);
        payload.put("hypothesisTitle", experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getTitle() : null);
        payload.put("hypothesisPersona", experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getPersona() : null);
        payload.put("defaultLocale", workflow.getSeedLocale() != null ? workflow.getSeedLocale() : DEFAULT_LOCALE);
        createJob(workflow, ExperimentAdSetJobType.AI_PREPARE_SEED, ExperimentAdSetWorker.AI, payload, null);
    }

    private void handleJobSuccess(ExperimentAdSetJob job) {
        ExperimentAdSetWorkflow workflow = reloadWorkflow(job);
        switch (job.getType()) {
            case AI_PREPARE_SEED -> handleSeedPlan(workflow, job);
            case FACEBOOK_SEED_LOOKUP -> handleSeedLookup(workflow, job);
            case FACEBOOK_TARGETING_SUGGESTIONS -> handleSuggestions(workflow);
            case FACEBOOK_SOCIAL_POSITIONS -> handlePositions(workflow);
            case AI_BUILD_SPECS -> handleSpecGeneration(workflow, job);
            case FACEBOOK_VALIDATE_SPEC -> handleSpecValidation(workflow, job);
            case FACEBOOK_REACH_ESTIMATE -> handleReachEstimate(workflow, job);
        }
    }

    private void handleJobFailure(ExperimentAdSetJob job, String errorMessage) {
        ExperimentAdSetWorkflow workflow = reloadWorkflow(job);
        workflow.setStatus(ExperimentAdSetWorkflowStatus.FAILED);
        workflow.setLastError(errorMessage);
        workflowRepository.save(workflow);
    }

    private void handleSeedPlan(ExperimentAdSetWorkflow workflow, ExperimentAdSetJob job) {
        JsonNode node = readJson(job.getResultPayload());
        String keyword = text(node, "seedKeyword");
        if (!StringUtils.hasText(keyword)) {
            markWorkflowFailed(workflow, "Seed principal não retornou palavra-chave");
            return;
        }
        String locale = StringUtils.hasText(text(node, "seedLocale")) ? text(node, "seedLocale") : DEFAULT_LOCALE;
        workflow.setSeedKeyword(keyword.trim());
        workflow.setSeedLocale(locale.trim());
        workflow.setAiNotes(node != null ? node.toString() : null);
        workflowRepository.save(workflow);
        enqueueSeedLookupJob(workflow, node);
    }

    private void enqueueSeedLookupJob(ExperimentAdSetWorkflow workflow, JsonNode seedNode) {
        String adAccountId;
        try {
            adAccountId = resolveAdAccountId();
        } catch (IllegalStateException ex) {
            markWorkflowFailed(workflow, ex.getMessage());
            return;
        }
        String query = null;
        ArrayNode searchTerms = seedNode != null && seedNode.has("searchTerms") && seedNode.get("searchTerms").isArray()
                ? (ArrayNode) seedNode.get("searchTerms")
                : null;
        if (searchTerms != null && searchTerms.size() > 0) {
            query = text(searchTerms.get(0));
        }
        if (!StringUtils.hasText(query)) {
            query = workflow.getSeedKeyword();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("locale", workflow.getSeedLocale() != null ? workflow.getSeedLocale() : DEFAULT_LOCALE);
        payload.put("limit", SEED_SEARCH_LIMIT);
        payload.put("country", DEFAULT_COUNTRY);
        payload.put("adAccountId", adAccountId);
        createJob(workflow, ExperimentAdSetJobType.FACEBOOK_SEED_LOOKUP, ExperimentAdSetWorker.FACEBOOK, payload, null);

        List<String> positionQueries = extractPositionQueries(workflow);
        if (!positionQueries.isEmpty()) {
            enqueuePositionsJob(workflow, adAccountId, positionQueries);
        }
    }

    private void enqueuePositionsJob(ExperimentAdSetWorkflow workflow, String adAccountId, List<String> queries) {
        if (jobRepository.existsByWorkflowIdAndTypeAndStatusIn(
                workflow.getId(),
                ExperimentAdSetJobType.FACEBOOK_SOCIAL_POSITIONS,
                EnumSet.of(ExperimentAdSetJobStatus.PENDING, ExperimentAdSetJobStatus.RUNNING))) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("queries", queries);
        payload.put("locale", workflow.getSeedLocale() != null ? workflow.getSeedLocale() : DEFAULT_LOCALE);
        payload.put("limit", POSITION_LIMIT);
        payload.put("adAccountId", adAccountId);
        createJob(workflow, ExperimentAdSetJobType.FACEBOOK_SOCIAL_POSITIONS, ExperimentAdSetWorker.FACEBOOK, payload, null);
    }

    private void handleSeedLookup(ExperimentAdSetWorkflow workflow, ExperimentAdSetJob job) {
        JsonNode node = readJson(job.getResultPayload());
        String interestId = text(node, "interestId");
        if (!StringUtils.hasText(interestId)) {
            markWorkflowFailed(workflow, "Meta não retornou ID para o seed");
            return;
        }
        workflow.setSeedInterestId(interestId.trim());
        workflow.setSeedInterestName(text(node, "interestName"));
        workflow.setSeedAudienceLower(asLong(node, "audienceLowerBound"));
        workflow.setSeedAudienceUpper(asLong(node, "audienceUpperBound"));
        workflowRepository.save(workflow);
        enqueueSuggestionJob(workflow);
    }

    private void enqueueSuggestionJob(ExperimentAdSetWorkflow workflow) {
        if (!StringUtils.hasText(workflow.getSeedInterestId())) {
            return;
        }
        if (jobRepository.existsByWorkflowIdAndTypeAndStatusIn(
                workflow.getId(),
                ExperimentAdSetJobType.FACEBOOK_TARGETING_SUGGESTIONS,
                EnumSet.of(ExperimentAdSetJobStatus.PENDING, ExperimentAdSetJobStatus.RUNNING))) {
            return;
        }
        String adAccountId;
        try {
            adAccountId = resolveAdAccountId();
        } catch (IllegalStateException ex) {
            markWorkflowFailed(workflow, ex.getMessage());
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("seedInterestId", workflow.getSeedInterestId());
        payload.put("seedInterestName", workflow.getSeedInterestName());
        payload.put("locale", workflow.getSeedLocale() != null ? workflow.getSeedLocale() : DEFAULT_LOCALE);
        payload.put("country", DEFAULT_COUNTRY);
        payload.put("limit", SUGGESTION_LIMIT);
        payload.put("adAccountId", adAccountId);
        createJob(workflow, ExperimentAdSetJobType.FACEBOOK_TARGETING_SUGGESTIONS, ExperimentAdSetWorker.FACEBOOK, payload, null);
    }

    private void handleSuggestions(ExperimentAdSetWorkflow workflow) {
        maybeScheduleSpecBuilder(workflow);
    }

    private void handlePositions(ExperimentAdSetWorkflow workflow) {
        maybeScheduleSpecBuilder(workflow);
    }

    private void maybeScheduleSpecBuilder(ExperimentAdSetWorkflow workflow) {
        if (!StringUtils.hasText(workflow.getSeedInterestId())) {
            return;
        }
        if (jobRepository.existsByWorkflowIdAndTypeAndStatusIn(
                workflow.getId(),
                ExperimentAdSetJobType.AI_BUILD_SPECS,
                EnumSet.of(ExperimentAdSetJobStatus.PENDING, ExperimentAdSetJobStatus.RUNNING))) {
            return;
        }
        Optional<ExperimentAdSetJob> suggestionsJob = jobRepository
                .findFirstByWorkflowIdAndTypeOrderByFinishedAtDesc(workflow.getId(), ExperimentAdSetJobType.FACEBOOK_TARGETING_SUGGESTIONS);
        if (suggestionsJob.isEmpty() || suggestionsJob.get().getStatus() != ExperimentAdSetJobStatus.SUCCEEDED) {
            return;
        }
        List<String> positionQueries = extractPositionQueries(workflow);
        if (!positionQueries.isEmpty()) {
            Optional<ExperimentAdSetJob> positionsJob = jobRepository
                    .findFirstByWorkflowIdAndTypeOrderByFinishedAtDesc(workflow.getId(), ExperimentAdSetJobType.FACEBOOK_SOCIAL_POSITIONS);
            if (positionsJob.isEmpty() || positionsJob.get().getStatus() != ExperimentAdSetJobStatus.SUCCEEDED) {
                return;
            }
        }
        Map<String, Object> payload = buildSpecBuilderPayload(workflow, suggestionsJob.get());
        createJob(workflow, ExperimentAdSetJobType.AI_BUILD_SPECS, ExperimentAdSetWorker.AI, payload, null);
    }

    private Map<String, Object> buildSpecBuilderPayload(ExperimentAdSetWorkflow workflow, ExperimentAdSetJob suggestionsJob) {
        Experiment experiment = loadExperiment(workflow);
        Map<String, Object> experimentNode = new LinkedHashMap<>();
        experimentNode.put("id", experiment.getId());
        experimentNode.put("name", experiment.getName());
        experimentNode.put("hypothesis", experiment.getHypothesis());
        experimentNode.put("nicheName", experiment.getNiche() != null ? experiment.getNiche().getName() : null);
        experimentNode.put("nicheSegment", experiment.getNiche() != null ? experiment.getNiche().getBaseSegmentation() : null);
        if (experiment.getHypothesisRef() != null) {
            experimentNode.put("hypothesisTitle", experiment.getHypothesisRef().getTitle());
            experimentNode.put("hypothesisPersona", experiment.getHypothesisRef().getPersona());
            experimentNode.put("hypothesisMechanism", experiment.getHypothesisRef().getMechanism());
        }
        Map<String, Object> seedNode = new LinkedHashMap<>();
        seedNode.put("keyword", workflow.getSeedKeyword());
        seedNode.put("locale", workflow.getSeedLocale() != null ? workflow.getSeedLocale() : DEFAULT_LOCALE);
        seedNode.put("interestId", workflow.getSeedInterestId());
        seedNode.put("interestName", workflow.getSeedInterestName());
        seedNode.put("audienceLower", workflow.getSeedAudienceLower());
        seedNode.put("audienceUpper", workflow.getSeedAudienceUpper());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("experiment", experimentNode);
        payload.put("seed", seedNode);
        payload.put("country", DEFAULT_COUNTRY);
        payload.put("suggestions", safeJsonNode(suggestionsJob.getResultPayload()));
        payload.put("positions", latestPositionsNode(workflow));
        payload.put("positionQueries", extractPositionQueries(workflow));
        return payload;
    }

    private JsonNode latestPositionsNode(ExperimentAdSetWorkflow workflow) {
        return jobRepository
                .findFirstByWorkflowIdAndTypeOrderByFinishedAtDesc(workflow.getId(), ExperimentAdSetJobType.FACEBOOK_SOCIAL_POSITIONS)
                .map(ExperimentAdSetJob::getResultPayload)
                .map(this::readJson)
                .orElseGet(objectMapper::createArrayNode);
    }

    private void handleSpecGeneration(ExperimentAdSetWorkflow workflow, ExperimentAdSetJob job) {
        JsonNode root = readJson(job.getResultPayload());
        JsonNode specsNode = root != null ? root.path("specs") : null;
        if (specsNode == null || !specsNode.isArray() || specsNode.isEmpty()) {
            markWorkflowFailed(workflow, "AI não retornou specs para o playbook");
            return;
        }
        specRepository.deleteByWorkflowId(workflow.getId());
        for (int i = 0; i < specsNode.size(); i++) {
            JsonNode specNode = specsNode.get(i);
            ExperimentAdSetSpec spec = new ExperimentAdSetSpec();
            spec.setWorkflow(workflow);
            spec.setSlot(resolveSlot(specNode.path("slot").asText(null), i));
            spec.setLabel(text(specNode, "label"));
            spec.setAgeMin(asInt(specNode, "ageMin"));
            spec.setAgeMax(asInt(specNode, "ageMax"));
            spec.setTargetingSpec(extractTargetingSpec(specNode.get("targetingSpec")));
            spec.setValidationStatus(null);
            spec.setValidationResponse(null);
            spec.setReachStatus(null);
            spec.setReachResponse(null);
            spec.setReachLowerBound(null);
            spec.setReachUpperBound(null);
            ExperimentAdSetSpec persisted = specRepository.save(spec);
            enqueueValidationJob(workflow, persisted);
        }
    }

    private void enqueueValidationJob(ExperimentAdSetWorkflow workflow, ExperimentAdSetSpec spec) {
        String adAccountId;
        try {
            adAccountId = resolveAdAccountId();
        } catch (IllegalStateException ex) {
            markWorkflowFailed(workflow, ex.getMessage());
            return;
        }
        JsonNode targeting = readJson(spec.getTargetingSpec());
        if (targeting == null || targeting.isNull()) {
            markWorkflowFailed(workflow, "Spec sem targeting JSON válido");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("specId", spec.getId());
        payload.put("slot", spec.getSlot() != null ? spec.getSlot().name() : null);
        payload.put("adAccountId", adAccountId);
        payload.put("targetingSpec", targeting);
        createJob(workflow, ExperimentAdSetJobType.FACEBOOK_VALIDATE_SPEC, ExperimentAdSetWorker.FACEBOOK, payload, spec.getId());
    }

    private void handleSpecValidation(ExperimentAdSetWorkflow workflow, ExperimentAdSetJob job) {
        Long specId = job.getResourceId();
        if (specId == null) {
            LOGGER.warn("Job {} não referencia spec para validação", job.getId());
            return;
        }
        ExperimentAdSetSpec spec = specRepository.findById(specId)
                .orElseThrow(() -> new EntityNotFoundException("Spec %d não encontrada".formatted(specId)));
        JsonNode result = readJson(job.getResultPayload());
        String validationStatus = result != null ? result.path("status").asText("VALID") : "VALID";
        spec.setValidationStatus(validationStatus);
        spec.setValidationResponse(result != null ? result.toString() : null);
        specRepository.save(spec);
        if (isValidValidationStatus(validationStatus)) {
            enqueueReachJob(workflow, spec);
        } else {
            skipReachForInvalidSpec(workflow, spec, result);
        }
    }

    private void enqueueReachJob(ExperimentAdSetWorkflow workflow, ExperimentAdSetSpec spec) {
        if (spec.getId() == null) {
            return;
        }
        if (jobRepository.existsByWorkflowIdAndTypeAndResourceIdAndStatusIn(
                workflow.getId(),
                ExperimentAdSetJobType.FACEBOOK_REACH_ESTIMATE,
                spec.getId(),
                EnumSet.of(ExperimentAdSetJobStatus.PENDING, ExperimentAdSetJobStatus.RUNNING))) {
            return;
        }
        String adAccountId;
        try {
            adAccountId = resolveAdAccountId();
        } catch (IllegalStateException ex) {
            markWorkflowFailed(workflow, ex.getMessage());
            return;
        }
        JsonNode targeting = readJson(spec.getTargetingSpec());
        if (targeting == null || targeting.isNull()) {
            markWorkflowFailed(workflow, "Spec sem targeting JSON válido");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("specId", spec.getId());
        payload.put("adAccountId", adAccountId);
        payload.put("targetingSpec", targeting);
        createJob(workflow, ExperimentAdSetJobType.FACEBOOK_REACH_ESTIMATE, ExperimentAdSetWorker.FACEBOOK, payload, spec.getId());
    }

    private void handleReachEstimate(ExperimentAdSetWorkflow workflow, ExperimentAdSetJob job) {
        Long specId = job.getResourceId();
        if (specId == null) {
            LOGGER.warn("Job {} sem spec associado para reach", job.getId());
            return;
        }
        ExperimentAdSetSpec spec = specRepository.findById(specId)
                .orElseThrow(() -> new EntityNotFoundException("Spec %d não encontrada".formatted(specId)));
        JsonNode result = readJson(job.getResultPayload());
        spec.setReachStatus(result != null ? result.path("status").asText("READY") : "READY");
        spec.setReachLowerBound(asLong(result, "usersLowerBound"));
        spec.setReachUpperBound(asLong(result, "usersUpperBound"));
        spec.setReachResponse(result != null ? result.toString() : null);
        specRepository.save(spec);
        checkWorkflowCompletion(workflow);
    }

    private boolean isValidValidationStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return "VALID".equalsIgnoreCase(status.trim());
    }

    private void skipReachForInvalidSpec(ExperimentAdSetWorkflow workflow,
                                          ExperimentAdSetSpec spec,
                                          JsonNode validationResult) {
        spec.setReachStatus("SKIPPED");
        spec.setReachLowerBound(null);
        spec.setReachUpperBound(null);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", "SKIPPED");
        node.put("reason", "Targeting inválido na validação do Facebook");
        if (validationResult != null) {
            node.set("validation", validationResult);
        }
        spec.setReachResponse(node.toString());
        specRepository.save(spec);
        checkWorkflowCompletion(workflow);
    }

    private void checkWorkflowCompletion(ExperimentAdSetWorkflow workflow) {
        List<ExperimentAdSetSpec> specs = specRepository.findByWorkflowId(workflow.getId());
        if (specs.isEmpty()) {
            return;
        }
        boolean allReady = specs.stream().allMatch(spec -> StringUtils.hasText(spec.getReachStatus()));
        if (allReady) {
            workflow.setStatus(ExperimentAdSetWorkflowStatus.COMPLETED);
            workflow.setCompletedAt(Instant.now());
            workflow.setLastError(null);
            workflowRepository.save(workflow);
        }
    }

    private ExperimentAdSetWorkflow reloadWorkflow(ExperimentAdSetJob job) {
        Long workflowId = job.getWorkflow() != null ? job.getWorkflow().getId() : null;
        if (workflowId == null) {
            throw new IllegalStateException("Job " + job.getId() + " não está associado a workflow");
        }
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("Workflow %d não encontrado".formatted(workflowId)));
    }

    private ExperimentAdSetSpecSlot resolveSlot(String raw, int index) {
        if (StringUtils.hasText(raw)) {
            try {
                return ExperimentAdSetSpecSlot.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fallback below
            }
        }
        return switch (index) {
            case 0 -> ExperimentAdSetSpecSlot.DESIGNERS;
            case 1 -> ExperimentAdSetSpecSlot.MARKETING;
            default -> ExperimentAdSetSpecSlot.SMB;
        };
    }

    private String extractTargetingSpec(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar targeting spec", e);
        }
    }

    private ExperimentAdSetJob createJob(ExperimentAdSetWorkflow workflow,
                                         ExperimentAdSetJobType type,
                                         ExperimentAdSetWorker worker,
                                         Object payload,
                                         Long resourceId) {
        ExperimentAdSetJob job = ExperimentAdSetJob.builder()
                .workflow(workflow)
                .type(type)
                .worker(worker)
                .status(ExperimentAdSetJobStatus.PENDING)
                .payload(writeJson(payload))
                .resourceId(resourceId)
                .build();
        return jobRepository.save(job);
    }

    private Experiment loadExperiment(ExperimentAdSetWorkflow workflow) {
        Experiment experiment = workflow.getExperiment();
        if (experiment != null && experiment.getId() != null) {
            return experiment;
        }
        Long experimentId = experiment != null ? experiment.getId() : null;
        if (experimentId == null) {
            throw new IllegalStateException("Workflow sem experimento associado");
        }
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
    }

    private List<String> extractPositionQueries(ExperimentAdSetWorkflow workflow) {
        JsonNode node = readJson(workflow.getAiNotes());
        if (node == null) {
            return List.of();
        }
        JsonNode queries = node.path("positionQueries");
        if (queries == null || !queries.isArray() || queries.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : queries) {
            String value = text(item);
            if (StringUtils.hasText(value)) {
                values.add(value.trim());
            }
        }
        return values.stream().distinct().limit(5).toList();
    }

    private JsonNode safeJsonNode(String raw) {
        JsonNode node = readJson(raw);
        return node != null ? node : objectMapper.createArrayNode();
    }

    private JsonNode readJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Falha ao converter JSON: {}", e.getMessage());
            return null;
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload", e);
        }
    }

    private void markWorkflowFailed(ExperimentAdSetWorkflow workflow, String message) {
        workflow.setStatus(ExperimentAdSetWorkflowStatus.FAILED);
        workflow.setLastError(message);
        workflowRepository.save(workflow);
        LOGGER.warn("Workflow {} marcado como falho: {}", workflow.getId(), message);
    }

    private String resolveAdAccountId() {
        return facebookAccountRepository.findFirstByWorkerEnabledTrue()
                .map(FacebookAccount::getAdAccountId)
                .map(this::normalizeAdAccountId)
                .orElseThrow(() -> new IllegalStateException("Nenhuma conta do Facebook com worker habilitado"));
    }

    private String normalizeAdAccountId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String trimmed = raw.trim();
        if (trimmed.regionMatches(true, 0, "act_", 0, 4)) {
            return "act_" + trimmed.substring(4);
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return "act_" + trimmed;
        }
        return trimmed;
    }

    private String text(JsonNode parent, String field) {
        if (parent == null) {
            return null;
        }
        return text(parent.get(field));
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText(null);
    }

    private Integer asInt(JsonNode parent, String field) {
        JsonNode node = parent != null ? parent.get(field) : null;
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.intValue();
        }
        try {
            return Integer.parseInt(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long asLong(JsonNode parent, String field) {
        JsonNode node = parent != null ? parent.get(field) : null;
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
