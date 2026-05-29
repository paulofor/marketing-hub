package com.marketinghub.geralanding.wireframe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
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

/** Responsável por adaptar consultas de execução para o contrato da etapa wireframe. */
@Service
public class BackendWireframeService {

    private static final Logger log = LoggerFactory.getLogger(BackendWireframeService.class);
    private static final TypeReference<LinkedHashMap<String, Object>> FRAMEWORK_TYPE = new TypeReference<>() {};
    private static final String STAGE_CODE = "landing-page-wireframe";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com os repositórios necessários para consultar execuções de wireframe. */
    public BackendWireframeService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    /** Inicia a execução manual da etapa wireframe usando o código canônico da etapa. */
    @Transactional
    public GeraLandingWireframeStartResponse start(Long experimentId) {
        return registerInitialExecution(experimentId, STAGE_CODE);
    }

    /** Registra a execução inicial da etapa convertendo para o DTO local de início. */
    private GeraLandingWireframeStartResponse registerInitialExecution(Long experimentId, String stageCode) {
        Instant now = Instant.now();
        var experiment = experimentRepository.findById(experimentId)
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
        return new GeraLandingWireframeStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    /** Lista execuções da etapa convertendo para o DTO local da etapa. */
    @Transactional(readOnly = true)
    public List<GeraLandingWireframeExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, String stageCode, boolean includeCompleted) {
        List<GeraLandingStageExecution> executions = includeCompleted
                ? executionRepository.findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode)
                : executionRepository.findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
                        experimentId,
                        stageCode,
                        STATUS_COMPLETED);
        return executions.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Lista os jobs iniciados da etapa wireframe para processamento independente de experimento. */
    @Transactional(readOnly = true)
    public List<RecordWireframePending> listPending(String stageCode) {
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(stageCode, STATUS_STARTED)
                .stream()
                .map(execution -> new RecordWireframePending(
                        execution.getExperimentId(),
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStageCode(),
                        toPendingExperiment(execution.getExperiment()),
                        toPendingHypothesis(execution.getExperiment())))
                .toList();
    }

    /** Converte o experimento da execução para os dados expostos na fila pending. */
    private RecordWireframeExperiment toPendingExperiment(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordWireframeExperiment(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                enumValueToText(experiment.getStatus()),
                enumValueToText(experiment.getStage()),
                experiment.getCreativeTextPrompt(),
                experiment.getCreativeImagePrompt(),
                resolveJsonArtifact(experiment.getId(), "campaignAngle", experiment.getCampaignAngle()),
                resolveJsonArtifact(experiment.getId(), "adCopy", experiment.getAdCopy()),
                resolveJsonArtifact(experiment.getId(), "adImageBriefing", experiment.getAdImageBriefing()),
                resolveJsonArtifact(experiment.getId(), "landingPageCopy", experiment.getLandingPageCopy()),
                resolveJsonArtifact(experiment.getId(), "landingPageWireframe", experiment.getLandingPageWireframe()),
                resolveJsonArtifact(experiment.getId(), "landingPageImagePlanning", experiment.getLandingPageImagePlanning()),
                resolveJsonArtifact(experiment.getId(), "landingPageDesignPreset", experiment.getLandingPageDesignPreset()),
                resolveJsonArtifact(experiment.getId(), "landingPageDeliverables", experiment.getLandingPageDeliverables()),
                experiment.getHtmlGeraLanding());
    }

    /** Preserva artefatos JSON como objetos estruturados na fila pending, mantendo textos não JSON intactos. */
    private Object resolveJsonArtifact(Long experimentId, String fieldName, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        String trimmed = rawValue.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return rawValue;
        }
        try {
            return objectMapper.readValue(trimmed, Object.class);
        } catch (JsonProcessingException ex) {
            log.warn(
                    "Falha ao ler artefato JSON no pending wireframe; mantendo texto bruto. experimentId={} fieldName={}",
                    experimentId,
                    fieldName,
                    ex);
            return rawValue;
        }
    }

    /** Converte a hipótese associada ao experimento para o framework exposto na fila pending. */
    private RecordWireframeHypothesis toPendingHypothesis(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordWireframeHypothesis(
                experiment.getHypothesisRefIdForPending(),
                experiment.getHypothesisRefTitleForPending(),
                resolveFramework(experiment.getId(), experiment.getHypothesisFrameworkJsonForPending()));
    }

    /** Resolve o JSON do framework da hipótese como objeto estruturado com todos os blocos canônicos. */
    private Map<String, Object> resolveFramework(Long experimentId, String frameworkJson) {
        Map<String, Object> framework = new LinkedHashMap<>();
        if (frameworkJson != null && !frameworkJson.isBlank()) {
            try {
                framework.putAll(objectMapper.readValue(frameworkJson, FRAMEWORK_TYPE));
            } catch (JsonProcessingException ex) {
                log.warn("Falha ao ler framework da hipótese no pending wireframe. experimentId={}", experimentId, ex);
            }
        }
        ensureFrameworkSections(framework);
        return framework;
    }

    /** Garante presença dos itens canônicos Dor, Resultado, Mecanismo, Prova, Oferta e checklist. */
    private void ensureFrameworkSections(Map<String, Object> framework) {
        framework.putIfAbsent("pain", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("result", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("mechanism", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("proof", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("offer", new LinkedHashMap<String, Object>());
        framework.putIfAbsent("checklist", new LinkedHashMap<String, Object>());
    }

    /** Converte um enum de experimento para texto sem acoplar o serviço às classes concretas do enum. */
    private String enumValueToText(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    @Transactional(readOnly = true)
    /** Retorna o detalhe da execução convertido para o DTO local da etapa. */
    public RecordBackendWireframeDetalheDto getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        return toDetailResponse(execution);
    }

    /** Converte o resumo transversal para o resumo local da etapa. */
    private GeraLandingWireframeExecutionSummaryResponse toSummaryResponse(GeraLandingStageExecution execution) {
        return new GeraLandingWireframeExecutionSummaryResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getCostUsd());
    }

    /** Converte o detalhe transversal para o detalhe local da etapa. */
    private RecordBackendWireframeDetalheDto toDetailResponse(GeraLandingStageExecution execution) {
        return new RecordBackendWireframeDetalheDto(
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
