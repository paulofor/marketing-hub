package com.marketinghub.geralanding.qualityreview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.detailStageExecution.RecordBackendQualityReviewDetalheDto;
import com.marketinghub.geralanding.qualityreview.service.listStageExecutions.GeraLandingQualityReviewExecutionSummaryResponse;
import com.marketinghub.geralanding.qualityreview.service.pending.RecordQualityReviewExperiment;
import com.marketinghub.geralanding.qualityreview.service.pending.RecordQualityReviewHypothesis;
import com.marketinghub.geralanding.qualityreview.service.pending.RecordQualityReviewPending;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por orquestrar a revisão visual assíncrona de qualidade comercial da landing gerada. */
@Service
public class BackendQualityReviewService {

    private static final Logger log = LoggerFactory.getLogger(BackendQualityReviewService.class);
    private static final TypeReference<LinkedHashMap<String, Object>> FRAMEWORK_TYPE = new TypeReference<>() {};
    private static final String STAGE_CODE = "landing-page-quality-review";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com repositórios e serializador usados pela revisão visual. */
    public BackendQualityReviewService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    /** Cria uma execução pendente do Quality Gate para avaliação posterior pelo Worker AI com modelo de visão. */
    @Transactional
    public GeraLandingQualityReviewStartResponse start(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        GeraLandingStageExecution execution = createExecution(experiment, "manual/start");
        return new GeraLandingQualityReviewStartResponse(fromDatabaseIdJob(execution.getIdJob()), execution.getStatus(), null);
    }

    /** Agenda automaticamente o Quality Gate visual após a montagem do HTML final do GeraLanding. */
    @Transactional
    public String reviewAfterHtmlGeneration(Experiment experiment) {
        GeraLandingStageExecution execution = createExecution(experiment, "auto/html-geralanding");
        return fromDatabaseIdJob(execution.getIdJob());
    }

    /** Lista execuções da etapa de revisão de qualidade para o experimento informado. */
    @Transactional(readOnly = true)
    public List<GeraLandingQualityReviewExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, boolean includeCompleted) {
        List<GeraLandingStageExecution> executions = includeCompleted
                ? executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, STAGE_CODE)
                : executionRepository.findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        experimentId,
                        STAGE_CODE,
                        STATUS_COMPLETED);
        return executions.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Lista os jobs iniciados da etapa quality-review para processamento pelo Worker AI. */
    @Transactional(readOnly = true)
    public List<RecordQualityReviewPending> listPending() {
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(STAGE_CODE, STATUS_STARTED)
                .stream()
                .map(execution -> new RecordQualityReviewPending(
                        execution.getExperimentId(),
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStageCode(),
                        execution.getExecutionRequestedAt(),
                        toPendingExperiment(execution.getExperiment()),
                        toPendingHypothesis(execution.getExperiment())))
                .toList();
    }

    /** Marca a execução como enviada à OpenAI após receber prompt, schema e request visual cru. */
    @Transactional
    public void markWaitingOpenAiDispatch(
            String idJob,
            String prompt,
            String promptMarkdownContent,
            String schemaJson,
            String requestBodyJson,
            String openAiJobId) {
        GeraLandingStageExecution execution = findByIdJob(idJob);
        execution.setPrompt(prompt);
        execution.setPromptMarkdownContent(StringUtils.hasText(promptMarkdownContent) ? promptMarkdownContent : prompt);
        execution.setSchemaJson(schemaJson);
        execution.setOpenAiRequestBody(requestBodyJson);
        execution.setOpenAiJobId(openAiJobId);
        execution.setProcessingStartedAt(Instant.now());
        execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        executionRepository.save(execution);
    }

    /** Conclui ou falha a execução com o diagnóstico visual retornado pelo Worker AI. */
    @Transactional
    public void markCompletedFromResponse(
            String idJob,
            Long experimentId,
            String stageCode,
            String modelResponse,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal costUsd,
            String openAiJobId,
            String errorMessage,
            String errorDetail) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .or(() -> executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding quality review execution not found for idJob: " + idJob));
        try {
            execution.setModelResponse(modelResponse);
            execution.setProvisionalHtml(modelResponse);
            if (StringUtils.hasText(openAiJobId)) {
                execution.setOpenAiJobId(openAiJobId);
            }
            execution.setInputTokens(inputTokens);
            execution.setOutputTokens(outputTokens);
            execution.setCostUsd(costUsd);
            String normalizedErrorDetail = StringUtils.hasText(errorDetail) ? errorDetail.trim() : null;
            String normalizedErrorMessage = normalizeErrorMessage(errorMessage, normalizedErrorDetail);
            execution.setErrorMessage(normalizedErrorMessage);
            execution.setErrorDetail(normalizedErrorDetail);
            execution.setCompletedAt(Instant.now());
            execution.setStatus(normalizedErrorMessage != null ? STATUS_FAILED : STATUS_COMPLETED);
            executionRepository.save(execution);
            if (normalizedErrorMessage == null) {
                persistQualityReviewArtifactOnExperiment(execution, modelResponse);
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao concluir Quality Gate visual (idJob={}, experimentId={}, stageCode={}, openAiJobId={}, modelResponseLength={}, errorMessage={})",
                    idJob,
                    experimentId,
                    stageCode,
                    openAiJobId,
                    modelResponse != null ? modelResponse.length() : 0,
                    errorMessage,
                    ex);
            throw ex;
        }
    }

    /** Retorna o detalhe persistido de uma execução específica da revisão de qualidade. */
    @Transactional(readOnly = true)
    public RecordBackendQualityReviewDetalheDto getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding quality review execution not found for idJob: " + idJob));
        return toDetailResponse(execution);
    }

    /** Cria o registro inicial da execução de Quality Gate visual com status pendente. */
    private GeraLandingStageExecution createExecution(Experiment experiment, String promptTemplateId) {
        Instant now = Instant.now();
        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(STAGE_CODE)
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId(promptTemplateId)
                .promptContent("Quality Gate visual da landing final via modelo de visão OpenAI.")
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        return executionRepository.save(execution);
    }

    /** Normaliza a mensagem de erro recebida no callback da revisão visual. */
    private String normalizeErrorMessage(String errorMessage, String normalizedErrorDetail) {
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage.trim();
        }
        if (StringUtils.hasText(normalizedErrorDetail)) {
            return "Falha ao processar Quality Gate visual da landing";
        }
        return null;
    }

    /** Persiste o JSON final da revisão visual no experimento associado à execução concluída. */
    private void persistQualityReviewArtifactOnExperiment(GeraLandingStageExecution execution, String modelResponse) {
        if (!StringUtils.hasText(modelResponse)) {
            return;
        }
        Experiment experiment = execution.getExperiment();
        if (experiment == null) {
            experiment = experimentRepository.findById(execution.getExperimentId())
                    .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + execution.getExperimentId()));
        }
        experiment.setLandingPageQualityReview(modelResponse);
        experimentRepository.save(experiment);
    }

    /** Converte o experimento da execução para os dados expostos na fila pending. */
    private RecordQualityReviewExperiment toPendingExperiment(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordQualityReviewExperiment(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                enumValueToText(experiment.getStatus()),
                enumValueToText(experiment.getStage()),
                resolveJsonArtifact(experiment.getId(), "campaignAngle", experiment.getCampaignAngle()),
                resolveJsonArtifact(experiment.getId(), "adCopy", experiment.getAdCopy()),
                resolveJsonArtifact(experiment.getId(), "adImageBriefing", experiment.getAdImageBriefing()),
                resolveJsonArtifact(experiment.getId(), "landingPageCopy", experiment.getLandingPageCopy()),
                resolveJsonArtifact(experiment.getId(), "landingPageWireframe", experiment.getLandingPageWireframe()),
                resolveJsonArtifact(experiment.getId(), "landingPageImagePlanning", experiment.getLandingPageImagePlanning()),
                resolveJsonArtifact(experiment.getId(), "landingPageImageAssets", experiment.getLandingPageImageAssets()),
                resolveJsonArtifact(experiment.getId(), "landingPageDesignPreset", experiment.getLandingPageDesignPreset()),
                resolveJsonArtifact(experiment.getId(), "landingPageDeliverables", experiment.getLandingPageDeliverables()),
                experiment.getHtmlGeraLanding(),
                experiment.getLandingPageHtml());
    }

    /** Converte artefato textual JSON para objeto estruturado quando possível. */
    private Object resolveJsonArtifact(Long experimentId, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return rawValue;
        }
        try {
            return objectMapper.readValue(trimmed, Object.class);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao ler artefato JSON no pending quality-review; mantendo texto bruto. experimentId={} fieldName={}", experimentId, fieldName, ex);
            return rawValue;
        }
    }

    /** Converte a hipótese associada ao experimento para o framework exposto na fila pending. */
    private RecordQualityReviewHypothesis toPendingHypothesis(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordQualityReviewHypothesis(
                experiment.getHypothesisRefIdForPending(),
                experiment.getHypothesisRefTitleForPending(),
                resolveFramework(experiment.getId(), experiment.getHypothesisFrameworkJsonForPending()));
    }

    /** Resolve o JSON do framework da hipótese garantindo as seções canônicas mínimas. */
    private Map<String, Object> resolveFramework(Long experimentId, String frameworkJson) {
        Map<String, Object> framework = new LinkedHashMap<>();
        if (StringUtils.hasText(frameworkJson)) {
            try {
                framework.putAll(objectMapper.readValue(frameworkJson, FRAMEWORK_TYPE));
            } catch (JsonProcessingException ex) {
                log.warn("Falha ao ler framework da hipótese no pending quality-review. experimentId={}", experimentId, ex);
            }
        }
        framework.putIfAbsent("pain", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("result", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("mechanism", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("proof", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("offer", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("checklist", new LinkedHashMap<String, Object>());
        return framework;
    }

    /** Busca uma execução pelo idJob textual informado no contrato interno. */
    private GeraLandingStageExecution findByIdJob(String idJob) {
        return executionRepository
                .findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding quality review execution not found for idJob: " + idJob));
    }

    /** Converte um enum de experimento para texto sem acoplar o serviço às classes concretas do enum. */
    private String enumValueToText(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    /** Converte o resumo persistido para o DTO público da etapa. */
    private GeraLandingQualityReviewExecutionSummaryResponse toSummaryResponse(GeraLandingStageExecution execution) {
        return new GeraLandingQualityReviewExecutionSummaryResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getCostUsd());
    }

    /** Converte a execução persistida para o detalhe público da etapa. */
    private RecordBackendQualityReviewDetalheDto toDetailResponse(GeraLandingStageExecution execution) {
        return new RecordBackendQualityReviewDetalheDto(
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

    /** Converte o id_job textual para o formato persistido em banco. */
    private byte[] toDatabaseIdJob(String idJob) {
        return idJob.getBytes(StandardCharsets.UTF_8);
    }

    /** Converte o id_job persistido em banco para texto. */
    private String fromDatabaseIdJob(byte[] idJob) {
        return new String(idJob, StandardCharsets.UTF_8);
    }
}
