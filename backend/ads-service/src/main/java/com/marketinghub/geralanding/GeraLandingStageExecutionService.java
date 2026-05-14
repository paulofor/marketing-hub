package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.pipeline.service.LandingPageImageInjector;
import com.marketinghub.experiment.repository.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeraLandingStageExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingStageExecutionService.class);
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_PROCESSING = "EM_PROCESSAMENTO";
    private static final String STATUS_FAILED = "FALHA";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STAGE_WIREFRAME = "landing-page-wireframe";
    private static final String STAGE_COPY = "landing-page-copy";
    private static final String STAGE_IMAGE_PLANNING = "landing-page-image-planning";
    private static final String STAGE_DESIGN_PRESET = "landing-page-design-preset";

    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final WireframeProvisionalHtmlAssembler wireframeProvisionalHtmlAssembler;
    private final CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler;
    private final LandingPageImageInjector landingPageImageInjector;

    public GeraLandingStageExecutionService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            WireframeProvisionalHtmlAssembler wireframeProvisionalHtmlAssembler,
            CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler,
            LandingPageImageInjector landingPageImageInjector) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.wireframeProvisionalHtmlAssembler = wireframeProvisionalHtmlAssembler;
        this.copyProvisionalHtmlAssembler = copyProvisionalHtmlAssembler;
        this.landingPageImageInjector = landingPageImageInjector;
    }

    @Transactional
    public GeraLandingStartResponse registerInitialExecution(Long experimentId, String stageCode) {
        Instant now = Instant.now();
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(stageCode)
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId("manual/start")
                .promptContent("Início manual via interface do experimento.")
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        GeraLandingStageExecution saved = executionRepository.save(execution);
        return new GeraLandingStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    @Transactional
    public String generateAndPersistProvisionalHtmlFromExperiment(Long experimentId, String jobId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

        if (!StringUtils.hasText(experiment.getLandingPageWireframe())) {
            throw new IllegalStateException("Não foi possível montar HTML provisório: experiment.landingPageWireframe ausente");
        }
        if (!StringUtils.hasText(experiment.getLandingPageCopy())) {
            throw new IllegalStateException("Não foi possível montar HTML provisório: experiment.landingPageCopy ausente");
        }

        String completeHtml = copyProvisionalHtmlAssembler.assembleComplete(
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                experiment.getLandingPageImagePlanning(),
                experiment.getLandingPageDesignPreset(),
                jobId);
        String htmlWithGeneratedImages = landingPageImageInjector.injectImages(experimentId, completeHtml);
        String provisionalHtml = """
                <!-- AUTO: provisional html generated manually by /geralanding/html/provisional/generate -->
                %s
                """.formatted(htmlWithGeneratedImages);

        experiment.setLandingPageHtml(provisionalHtml);
        experimentRepository.save(experiment);
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(jobId))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + jobId));
        execution.setProvisionalHtml(provisionalHtml);
        executionRepository.save(execution);
        return provisionalHtml;
    }

    @Transactional
    public void registerWorkerPromptExecution(GeraLandingWorkerPromptRequest request) {
        Instant now = Instant.now();
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + request.experimentId()));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(request.stageCode())
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId(request.jobId())
                .promptContent(request.promptContent())
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(request.jobId()))
                .build();
        executionRepository.save(execution);
    }

    @Transactional
    public void receivePrompt(String idJob, GeraLandingPromptReceiveRequest request) {
        log.info(
                "Receiving gera-landing prompt. idJob={}, experimentId={}, stageCode={}, promptLength={}",
                idJob,
                request.experimentId(),
                request.stageCode(),
                request.prompt() != null ? request.prompt().length() : 0);

        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> fallbackExecutionByExperimentAndStage(request))
                .orElseThrow(() -> {
                    log.error(
                            "GeraLanding execution not found before prompt persistence. idJob={}, experimentId={}, stageCode={}",
                            idJob,
                            request.experimentId(),
                            request.stageCode());
                    return new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob);
                });

        log.info(
                "GeraLanding execution found. idJob={}, persistedExperimentId={}, persistedStageCode={}, previousStatus={}",
                idJob,
                execution.getExperimentId(),
                execution.getStageCode(),
                execution.getStatus());

        Instant now = Instant.now();
        execution.setPrompt(request.prompt());
        execution.setOpenAiJobId(request.openAiJobId());
        execution.setOpenAiRequestBody(request.openAiRequestBody());
        execution.setOpenAiModel(request.openAiModel());
        execution.setSchemaJson(request.schemaJson());
        execution.setPromptMarkdownContent(request.promptMarkdownContent());
        execution.setProcessingStartedAt(now);
        execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        executionRepository.save(execution);
        log.info("GeraLanding prompt persisted. idJob={}, newStatus={}", idJob, execution.getStatus());
    }

    @Transactional
    public void markAsSentToOpenAi(String idJob, GeraLandingDispatchReceiveRequest request) {
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(request.experimentId(), request.stageCode()))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        execution.setOpenAiJobId(request.openAiJobId());
        execution.setStatus(STATUS_PROCESSING);
        executionRepository.save(execution);
    }

    private java.util.Optional<GeraLandingStageExecution> fallbackExecutionByExperimentAndStage(
            GeraLandingPromptReceiveRequest request) {
        log.warn(
                "Primary lookup by idJob failed. Trying fallback by experiment/stage. experimentId={}, stageCode={}",
                request.experimentId(),
                request.stageCode());
        return executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                request.experimentId(),
                request.stageCode());
    }




    @Transactional
    public void receiveResult(String idJob, GeraLandingResultReceiveRequest request) {
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(request.experimentId(), request.stageCode()))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        execution.setModelResponse(request.modelResponse());
        String provisionalHtml = resolveProvisionalHtml(idJob, request, execution);
        execution.setProvisionalHtml(provisionalHtml);
        execution.setErrorMessage(StringUtils.hasText(request.errorMessage()) ? request.errorMessage().trim() : null);
        execution.setErrorDetail(StringUtils.hasText(request.errorDetail()) ? request.errorDetail().trim() : null);
        if (request.openAiJobId() != null && !request.openAiJobId().isBlank()) {
            execution.setOpenAiJobId(request.openAiJobId());
        }
        execution.setInputTokens(request.inputTokens());
        execution.setOutputTokens(request.outputTokens());
        execution.setCostUsd(request.costUsd());
        execution.setCompletedAt(Instant.now());
        execution.setStatus(StringUtils.hasText(request.errorMessage()) ? STATUS_FAILED : STATUS_COMPLETED);
        executionRepository.save(execution);
        persistStageArtifactOnExperiment(request, execution);
    }


    private String resolveProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        if (StringUtils.hasText(request.provisionalHtml())) {
            return request.provisionalHtml();
        }
        if (STAGE_COPY.equalsIgnoreCase(request.stageCode())) {
            Experiment experiment = execution.getExperiment();
            if (experiment == null && request.experimentId() != null) {
                experiment = experimentRepository.findById(request.experimentId()).orElse(null);
            }
            String wireframe = experiment != null ? experiment.getLandingPageWireframe() : null;
            if (!StringUtils.hasText(wireframe)) {
                throw new IllegalStateException("Não foi possível montar HTML provisório da copy: experiment.landingPageWireframe ausente");
            }
            return copyProvisionalHtmlAssembler.assemble(request.modelResponse(), wireframe, idJob);
        }
        if (STAGE_WIREFRAME.equalsIgnoreCase(request.stageCode())) {
            return wireframeProvisionalHtmlAssembler.assemble(request.modelResponse(), idJob);
        }
        if (STAGE_IMAGE_PLANNING.equalsIgnoreCase(request.stageCode())) {
            return resolveImagePlanningProvisionalHtml(idJob, request, execution);
        }
        if (STAGE_DESIGN_PRESET.equalsIgnoreCase(request.stageCode())) {
            return resolveDesignPresetProvisionalHtml(idJob, request, execution);
        }
        return null;
    }

    private String resolveImagePlanningProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        Experiment experiment = execution.getExperiment();
        if (experiment == null && request.experimentId() != null) {
            experiment = experimentRepository.findById(request.experimentId()).orElse(null);
        }
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe()) || !StringUtils.hasText(experiment.getLandingPageCopy())) {
            return null;
        }
        String baseHtml = copyProvisionalHtmlAssembler.assemble(
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                idJob);
        if (!StringUtils.hasText(baseHtml)) {
            return null;
        }
        String htmlWithGeneratedImages = landingPageImageInjector.injectImages(experiment.getId(), baseHtml);
        return """
                <!-- AUTO: provisional html regenerated after landing-page-image-planning completion -->
                %s
                """.formatted(htmlWithGeneratedImages);
    }

    private String resolveDesignPresetProvisionalHtml(String idJob, GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        Experiment experiment = execution.getExperiment();
        if (experiment == null && request.experimentId() != null) {
            experiment = experimentRepository.findById(request.experimentId()).orElse(null);
        }
        if (experiment == null || !StringUtils.hasText(experiment.getLandingPageWireframe()) || !StringUtils.hasText(experiment.getLandingPageCopy())) {
            return null;
        }
        String completeHtml = copyProvisionalHtmlAssembler.assembleComplete(
                experiment.getLandingPageCopy(),
                experiment.getLandingPageWireframe(),
                experiment.getLandingPageImagePlanning(),
                request.modelResponse(),
                idJob);
        if (!StringUtils.hasText(completeHtml)) {
            return null;
        }
        String htmlWithGeneratedImages = landingPageImageInjector.injectImages(experiment.getId(), completeHtml);
        return """
                <!-- AUTO: provisional html regenerated after landing-page-design-preset completion -->
                %s
                """.formatted(htmlWithGeneratedImages);
    }

    private void persistStageArtifactOnExperiment(GeraLandingResultReceiveRequest request, GeraLandingStageExecution execution) {
        String stageCode = request.stageCode();
        if (!STAGE_WIREFRAME.equalsIgnoreCase(stageCode)
                && !STAGE_COPY.equalsIgnoreCase(stageCode)
                && !STAGE_IMAGE_PLANNING.equalsIgnoreCase(stageCode)
                && !STAGE_DESIGN_PRESET.equalsIgnoreCase(stageCode)) {
            return;
        }
        if (!StringUtils.hasText(request.modelResponse()) || StringUtils.hasText(request.errorMessage())) {
            return;
        }
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + request.experimentId()));
        if (STAGE_WIREFRAME.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageWireframe(request.modelResponse());
            experiment.setLandingPageWireframeJobId(execution.getIdJob());
        } else if (STAGE_COPY.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageCopy(request.modelResponse());
            experiment.setLandingPageCopyJobId(execution.getIdJob());
        } else if (STAGE_IMAGE_PLANNING.equalsIgnoreCase(stageCode)) {
            experiment.setLandingPageImagePlanning(request.modelResponse());
            if (StringUtils.hasText(execution.getProvisionalHtml())) {
                experiment.setLandingPageHtml(execution.getProvisionalHtml());
            }
        } else {
            experiment.setLandingPageDesignPreset(request.modelResponse());
            if (StringUtils.hasText(execution.getProvisionalHtml())) {
                experiment.setLandingPageHtml(execution.getProvisionalHtml());
            }
        }
        experimentRepository.save(experiment);
    }

    @Transactional(readOnly = true)
    public List<GeraLandingExecutionSummaryResponse> listExperimentStageExecutions(
            Long experimentId,
            String stageCode,
            boolean includeCompleted) {
        List<GeraLandingStageExecution> executions = includeCompleted
                ? executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode)
                : executionRepository.findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        experimentId,
                        stageCode,
                        STATUS_COMPLETED);
        return executions
                .stream()
                .map(execution -> new GeraLandingExecutionSummaryResponse(
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStatus(),
                        execution.getExecutionRequestedAt(),
                        execution.getCostUsd()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GeraLandingStageExecutionDetailResponse getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));

        return new GeraLandingStageExecutionDetailResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getExperimentId(),
                execution.getStageCode(),
                execution.getExecutionRequestedAt(),
                execution.getCreatedAt(),
                execution.getProcessingStartedAt(),
                execution.getCompletedAt(),
                execution.getPromptTemplateId(),
                execution.getPromptContent(),
                execution.getPrompt(),
                execution.getOpenAiRequestBody(),
                execution.getOpenAiModel(),
                execution.getSchemaJson(),
                execution.getPromptMarkdownContent(),
                execution.getStatus(),
                execution.getOpenAiJobId(),
                execution.getModelResponse(),
                execution.getProvisionalHtml(),
                execution.getErrorMessage(),
                execution.getErrorDetail(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getCostUsd());
    }

    @Transactional(readOnly = true)
    public List<GeraLandingPendingExecutionResponse> listPendingExecutions() {
        return executionRepository.findTop20ByStatusInOrderByExecutionRequestedAtAsc(List.of(STATUS_STARTED, STATUS_WAITING_OPENAI_DISPATCH))
                .stream()
                        .map(execution -> new GeraLandingPendingExecutionResponse(
                                execution.getExperimentId(),
                                fromDatabaseIdJob(execution.getIdJob()),
                                execution.getStageCode()))
                .toList();
    }

    private byte[] toDatabaseIdJob(String idJob) {
        return idJob.getBytes(StandardCharsets.UTF_8);
    }

    private String fromDatabaseIdJob(byte[] idJob) {
        return new String(idJob, StandardCharsets.UTF_8);
    }
}
