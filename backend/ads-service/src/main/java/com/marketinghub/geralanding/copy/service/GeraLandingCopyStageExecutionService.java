package com.marketinghub.geralanding.copy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.geralanding.copy.service.detailStageExecution.RecordBackendCopyDetalheDto;
import com.marketinghub.geralanding.copy.service.listStageExecutions.GeraLandingCopyExecutionSummaryResponse;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyExperiment;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyHypothesis;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyPending;
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

/** Responsável por adaptar consultas de execução para o contrato da etapa copy. */
@Service
public class GeraLandingCopyStageExecutionService {

    private static final Logger log = LoggerFactory.getLogger(GeraLandingCopyStageExecutionService.class);
    private static final TypeReference<LinkedHashMap<String, Object>> FRAMEWORK_TYPE = new TypeReference<>() {};
    private static final String STAGE_CODE = "landing-page-copy";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com os repositórios necessários para consultar execuções de copy. */
    public GeraLandingCopyStageExecutionService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    /** Inicia a execução manual da etapa copy usando o código canônico da etapa. */
    @Transactional
    public GeraLandingCopyStartResponse start(Long experimentId) {
        return registerInitialExecution(experimentId, STAGE_CODE);
    }

    /** Registra a execução inicial da etapa convertendo para o DTO local de início. */
    @Transactional
    public GeraLandingCopyStartResponse registerInitialExecution(Long experimentId, String stageCode) {
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
        return new GeraLandingCopyStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    /** Lista execuções da etapa convertendo para o DTO local da etapa. */
    @Transactional(readOnly = true)
    public List<GeraLandingCopyExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, String stageCode, boolean includeCompleted) {
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

    /** Lista os jobs iniciados da etapa copy para processamento independente de experimento. */
    @Transactional(readOnly = true)
    public List<RecordCopyPending> listPending(String stageCode) {
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(stageCode, STATUS_STARTED)
                .stream()
                .map(execution -> new RecordCopyPending(
                        execution.getExperimentId(),
                        fromDatabaseIdJob(execution.getIdJob()),
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStageCode(),
                        execution.getStatus(),
                        toPendingExperiment(execution.getExperiment()),
                        toPendingHypothesis(execution.getExperiment())))
                .toList();
    }

    /** Marca a execução como preparada após receber prompt, schema e request cru despachados para IA. */
    @Transactional
    public void markPromptReceived(
            String idJob,
            String prompt,
            String promptMarkdownContent,
            String schemaJson,
            String openAiRequestBody,
            String openAiModel,
            String openAiJobId) {
        GeraLandingStageExecution execution = findByJob(idJob);
        execution.setPrompt(prompt);
        execution.setPromptMarkdownContent(resolvePromptMarkdownContent(prompt, promptMarkdownContent));
        execution.setSchemaJson(schemaJson);
        execution.setOpenAiRequestBody(openAiRequestBody);
        execution.setOpenAiModel(openAiModel);
        if (StringUtils.hasText(openAiJobId)) {
            execution.setOpenAiJobId(openAiJobId);
            execution.setProcessingStartedAt(Instant.now());
            execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        }
        executionRepository.save(execution);
    }

    /** Marca a execução como aguardando retorno da OpenAI após receber o identificador do job remoto. */
    @Transactional
    public void markWaitingOpenAiDispatch(String idJob, String openAiJobId) {
        GeraLandingStageExecution execution = findByJob(idJob);
        if (StringUtils.hasText(openAiJobId)) {
            execution.setOpenAiJobId(openAiJobId);
        }
        execution.setProcessingStartedAt(Instant.now());
        execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        executionRepository.save(execution);
    }

    /** Resolve o markdown bruto do prompt mantendo compatibilidade com clientes antigos que enviavam esse conteúdo em prompt. */
    private String resolvePromptMarkdownContent(String prompt, String promptMarkdownContent) {
        if (promptMarkdownContent != null && !promptMarkdownContent.isBlank()) {
            return promptMarkdownContent;
        }
        return prompt;
    }

    /** Conclui ou falha a execução da etapa copy com a resposta devolvida pelo Worker AI. */
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
                .or(() -> executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                        experimentId,
                        stageCode))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        try {
            execution.setModelResponse(modelResponse);
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
                persistCopyArtifactOnExperiment(execution, modelResponse);
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao concluir resposta copy (idJob={}, experimentId={}, stageCode={}, openAiJobId={}, modelResponseLength={}, errorMessage={})",
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

    /** Retorna o detalhe da execução convertido para o DTO local da etapa. */
    @Transactional(readOnly = true)
    public RecordBackendCopyDetalheDto getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        return toDetailResponse(execution);
    }

    /** Busca uma execução pelo identificador textual do job. */
    private GeraLandingStageExecution findByJob(String idJob) {
        return executionRepository
                .findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
    }

    /** Normaliza a mensagem de erro e garante status FALHA quando o callback traz apenas detalhe técnico. */
    private String normalizeErrorMessage(String errorMessage, String normalizedErrorDetail) {
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage.trim();
        }
        if (StringUtils.hasText(normalizedErrorDetail)) {
            return "Falha ao processar etapa copy";
        }
        return null;
    }

    /** Persiste o artefato JSON final do copy no experimento associado à execução concluída. */
    private void persistCopyArtifactOnExperiment(GeraLandingStageExecution execution, String modelResponse) {
        if (!StringUtils.hasText(modelResponse)) {
            return;
        }
        Experiment experiment = execution.getExperiment();
        if (experiment == null) {
            experiment = experimentRepository.findById(execution.getExperimentId())
                    .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + execution.getExperimentId()));
        }
        experiment.setLandingPageCopy(modelResponse);
        experimentRepository.save(experiment);
    }

    /** Converte o experimento da execução para os dados expostos na fila pending. */
    private RecordCopyExperiment toPendingExperiment(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordCopyExperiment(
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
                    "Falha ao ler artefato JSON no pending copy; mantendo texto bruto. experimentId={} fieldName={}",
                    experimentId,
                    fieldName,
                    ex);
            return rawValue;
        }
    }

    /** Converte a hipótese associada ao experimento para o framework exposto na fila pending. */
    private RecordCopyHypothesis toPendingHypothesis(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordCopyHypothesis(
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
                log.warn("Falha ao ler framework da hipótese no pending copy. experimentId={}", experimentId, ex);
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

    /** Converte o resumo transversal para o resumo local da etapa. */
    private GeraLandingCopyExecutionSummaryResponse toSummaryResponse(GeraLandingStageExecution execution) {
        return new GeraLandingCopyExecutionSummaryResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getCostUsd());
    }

    /** Converte o detalhe transversal para o detalhe local da etapa. */
    private RecordBackendCopyDetalheDto toDetailResponse(GeraLandingStageExecution execution) {
        return new RecordBackendCopyDetalheDto(
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
