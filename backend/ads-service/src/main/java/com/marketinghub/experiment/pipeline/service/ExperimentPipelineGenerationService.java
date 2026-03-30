package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStage;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.pipeline.repository.ExperimentPipelineGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.openai.OpenAiCostEstimator;
import com.marketinghub.openai.OpenAiResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExperimentPipelineGenerationService {
    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final Duration STALE_PENDING_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration STALE_PROCESSING_TIMEOUT = Duration.ofMinutes(20);

    private final ExperimentRepository experimentRepository;
    private final ExperimentPipelineGenerationJobRepository jobRepository;
    private final ExperimentMapper experimentMapper;
    private final AiWorkerGenerationService generationService;
    private final ObjectMapper objectMapper;

    public ExperimentPipelineGenerationService(ExperimentRepository experimentRepository,
                                               ExperimentPipelineGenerationJobRepository jobRepository,
                                               ExperimentMapper experimentMapper,
                                               AiWorkerGenerationService generationService,
                                               ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.jobRepository = jobRepository;
        this.experimentMapper = experimentMapper;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExperimentDto generate(Long experimentId,
                                  ExperimentPipelineSection section,
                                  ExperimentPipelineGenerationRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));

        validatePredecessor(experiment, section);

        List<ExperimentPipelineGenerationJob> activeJobs = jobRepository
                .findByExperimentIdAndSectionAndStatusInOrderByCreatedAtDesc(
                        experimentId,
                        section,
                        Set.of(ExperimentPipelineGenerationJobStatus.PENDING, ExperimentPipelineGenerationJobStatus.PROCESSING));
        boolean hasActiveJob = markStaleJobsAsFailed(activeJobs);
        if (!hasActiveJob) {
            enqueueJob(experiment, section, request);
        }
        return experimentMapper.toDto(experiment);
    }

    @Transactional(readOnly = true)
    public List<ExperimentPipelineGenerationJobDto> listPendingJobs(int limit) {
        return jobRepository.findByStatusOrderByCreatedAtAsc(ExperimentPipelineGenerationJobStatus.PENDING,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExperimentPipelineGenerationJobDto> listJobs(Long experimentId, int limit) {
        return jobRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ExperimentPipelineGenerationJobDto claimJob(UUID jobId, String workerId) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() != ExperimentPipelineGenerationJobStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job não está pendente");
        }
        job.setStatus(ExperimentPipelineGenerationJobStatus.PROCESSING);
        job.setStage(ExperimentPipelineGenerationJobStage.SENT_TO_OPENAI);
        job.setWorkerId(StringUtils.hasText(workerId) ? workerId.trim() : "unknown-worker");
        job.setStartedAt(Instant.now());
        return toDto(job);
    }

    @Transactional
    public void updateJobStage(UUID jobId, ExperimentPipelineGenerationJobStage stage) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == ExperimentPipelineGenerationJobStatus.COMPLETED
                || job.getStatus() == ExperimentPipelineGenerationJobStatus.FAILED) {
            return;
        }
        job.setStage(stage != null ? stage : job.getStage());
    }

    @Transactional
    public void completeJob(UUID jobId, ExperimentPipelineGenerationJobCompletionRequest request) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == ExperimentPipelineGenerationJobStatus.COMPLETED) {
            return;
        }
        Experiment experiment = job.getExperiment();
        applySectionContent(experiment, job.getSection(), request.responseContent());

        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(
                request.inputTokens(),
                request.outputTokens(),
                request.inputTokens(),
                request.outputTokens(),
                totalTokens(request.inputTokens(), request.outputTokens()));
        BigDecimal estimatedCost = request.costUsd() != null
                ? request.costUsd()
                : OpenAiCostEstimator.estimateUsd(job.getModel(), usage);

        generationService.recordGeneration(AiWorkerGenerationRequest.builder()
                .domain("experiment.pipeline." + job.getSection().path())
                .referenceId(job.getExperiment().getId().toString())
                .prompt(job.getPrompt())
                .rawResponse(request.rawResponse())
                .model(job.getModel())
                .inputTokens(request.inputTokens())
                .outputTokens(request.outputTokens())
                .costUsd(estimatedCost)
                .build());

        job.setStatus(ExperimentPipelineGenerationJobStatus.COMPLETED);
        job.setStage(ExperimentPipelineGenerationJobStage.COMPLETED);
        if (StringUtils.hasText(request.requestBodyJson())) {
            job.setRequestBodyJson(request.requestBodyJson().trim());
        }
        job.setResponseContent(request.responseContent());
        job.setRawResponse(request.rawResponse());
        job.setInputTokens(request.inputTokens());
        job.setOutputTokens(request.outputTokens());
        job.setCostUsd(estimatedCost);
        job.setErrorMessage(null);
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        ExperimentPipelineGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        if (job.getStatus() == ExperimentPipelineGenerationJobStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job já finalizado");
        }
        job.setStatus(ExperimentPipelineGenerationJobStatus.FAILED);
        job.setStage(ExperimentPipelineGenerationJobStage.FAILED);
        job.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Falha desconhecida");
        job.setFinishedAt(Instant.now());
    }

    private void enqueueJob(Experiment experiment,
                            ExperimentPipelineSection section,
                            ExperimentPipelineGenerationRequest request) {
        String model = StringUtils.hasText(request.getModel()) ? request.getModel().trim() : DEFAULT_MODEL;
        String userPrompt = buildUserPrompt(experiment, section, request.getCustomInstructions());
        Map<String, Object> requestBody = buildRequestBody(model, userPrompt, section);

        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao serializar job de geração", ex);
        }

        ExperimentPipelineGenerationJob job = ExperimentPipelineGenerationJob.builder()
                .experiment(experiment)
                .section(section)
                .status(ExperimentPipelineGenerationJobStatus.PENDING)
                .stage(ExperimentPipelineGenerationJobStage.WAITING_AI_WORKER)
                .model(model)
                .customInstructions(request.getCustomInstructions())
                .prompt(userPrompt)
                .requestBodyJson(requestBodyJson)
                .build();
        jobRepository.save(job);
    }

    private void validatePredecessor(Experiment experiment, ExperimentPipelineSection section) {
        ExperimentPipelineSection predecessor = section.predecessor();
        if (predecessor == null) {
            return;
        }
        String predecessorContent = switch (predecessor) {
            case CAMPAIGN_ANGLE -> experiment.getCampaignAngle();
            case AD_COPY -> experiment.getAdCopy();
            case AD_IMAGE_BRIEFING -> experiment.getAdImageBriefing();
            case LANDING_PAGE_COPY -> experiment.getLandingPageCopy();
            case LANDING_PAGE_WIREFRAME -> experiment.getLandingPageWireframe();
        };
        if (!StringUtils.hasText(predecessorContent)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A seção " + section.path() + " depende da seção " + predecessor.path() + " já concluída");
        }
    }

    private boolean markStaleJobsAsFailed(List<ExperimentPipelineGenerationJob> activeJobs) {
        Instant now = Instant.now();
        boolean hasActiveJob = false;
        for (ExperimentPipelineGenerationJob activeJob : activeJobs) {
            if (isStale(activeJob, now)) {
                activeJob.setStatus(ExperimentPipelineGenerationJobStatus.FAILED);
                activeJob.setStage(ExperimentPipelineGenerationJobStage.FAILED);
                activeJob.setErrorMessage("Job anterior expirou aguardando processamento. Uma nova solicitação foi liberada.");
                activeJob.setFinishedAt(now);
                continue;
            }
            hasActiveJob = true;
        }
        return hasActiveJob;
    }

    private boolean isStale(ExperimentPipelineGenerationJob job, Instant now) {
        Duration timeout = job.getStatus() == ExperimentPipelineGenerationJobStatus.PROCESSING
                ? STALE_PROCESSING_TIMEOUT
                : STALE_PENDING_TIMEOUT;
        Instant reference = job.getStartedAt() != null ? job.getStartedAt() : job.getCreatedAt();
        if (reference == null) {
            return false;
        }
        return reference.plus(timeout).isBefore(now);
    }

    private String buildUserPrompt(Experiment experiment,
                                   ExperimentPipelineSection section,
                                   String customInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Experimento #").append(experiment.getId()).append("\n");
        sb.append("Nome do experimento: ").append(nonBlank(experiment.getName())).append("\n");
        sb.append("Hipótese resumida: ").append(nonBlank(experiment.getHypothesis())).append("\n");
        if (experiment.getHypothesisRef() != null) {
            sb.append("Título da hipótese: ").append(nonBlank(experiment.getHypothesisRef().getTitle())).append("\n");
            sb.append("Persona: ").append(nonBlank(experiment.getHypothesisRef().getPersona())).append("\n");
            sb.append("Problema: ").append(nonBlank(experiment.getHypothesisRef().getProblem())).append("\n");
            sb.append("Promessa: ").append(nonBlank(experiment.getHypothesisRef().getPromise())).append("\n");
        }
        sb.append("\nTarefa alvo: ").append(section.path()).append("\n");
        appendPreviousOutputs(sb, experiment, section);
        if (StringUtils.hasText(customInstructions)) {
            sb.append("\nInstruções extras do usuário:\n").append(customInstructions.trim()).append("\n");
        }
        sb.append("\nResponda exclusivamente em JSON válido no formato {\"content\":\"...\"}.");
        return sb.toString();
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 String userPrompt,
                                                 ExperimentPipelineSection section) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", buildSystemPrompt(section)),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", "experiment_pipeline_content",
                        "strict", true,
                        "schema", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "content", Map.of("type", "string")
                                ),
                                "required", List.of("content")
                        )
                )
        ));
        return body;
    }

    private String buildSystemPrompt(ExperimentPipelineSection section) {
        return "Você é especialista em marketing direto e criação de ativos para performance. "
                + "Escreva em português do Brasil, de forma clara e vendável. "
                + "Seção atual: " + section.path() + ". "
                + "Considere as dependências anteriores já geradas e mantenha consistência entre elas.";
    }

    private void appendPreviousOutputs(StringBuilder sb,
                                       Experiment experiment,
                                       ExperimentPipelineSection section) {
        if (section.predecessor() == null) {
            return;
        }
        if (StringUtils.hasText(experiment.getCampaignAngle())) {
            sb.append("\nÂngulo da campanha:\n").append(experiment.getCampaignAngle().trim()).append("\n");
        }
        if (StringUtils.hasText(experiment.getAdCopy())) {
            sb.append("\nTexto do anúncio:\n").append(experiment.getAdCopy().trim()).append("\n");
        }
        if (StringUtils.hasText(experiment.getAdImageBriefing())) {
            sb.append("\nBriefing da imagem:\n").append(experiment.getAdImageBriefing().trim()).append("\n");
        }
        if (StringUtils.hasText(experiment.getLandingPageCopy())) {
            sb.append("\nTextos da landing:\n").append(experiment.getLandingPageCopy().trim()).append("\n");
        }
    }

    private void applySectionContent(Experiment experiment,
                                     ExperimentPipelineSection section,
                                     String content) {
        String normalized = StringUtils.hasText(content) ? content.trim() : null;
        switch (section) {
            case CAMPAIGN_ANGLE -> experiment.setCampaignAngle(normalized);
            case AD_COPY -> experiment.setAdCopy(normalized);
            case AD_IMAGE_BRIEFING -> experiment.setAdImageBriefing(normalized);
            case LANDING_PAGE_COPY -> experiment.setLandingPageCopy(normalized);
            case LANDING_PAGE_WIREFRAME -> experiment.setLandingPageWireframe(normalized);
        }
    }

    private ExperimentPipelineGenerationJobDto toDto(ExperimentPipelineGenerationJob job) {
        return ExperimentPipelineGenerationJobDto.builder()
                .id(job.getId())
                .experimentId(job.getExperiment().getId())
                .section(job.getSection())
                .status(job.getStatus().name())
                .stage(job.getStage().name())
                .customInstructions(job.getCustomInstructions())
                .errorMessage(job.getErrorMessage())
                .model(job.getModel())
                .prompt(job.getPrompt())
                .requestBodyJson(job.getRequestBodyJson())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private Integer totalTokens(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        return (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
    }

    private String nonBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
