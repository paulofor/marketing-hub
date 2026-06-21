package com.marketinghub.geralanding.imageplanning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.geralanding.imageplanning.service.detailStageExecution.RecordBackendImagePlanningDetalheDto;
import com.marketinghub.geralanding.imageplanning.service.listStageExecutions.GeraLandingImagePlanningExecutionSummaryResponse;
import com.marketinghub.geralanding.imageplanning.service.pending.RecordImagePlanningExperiment;
import com.marketinghub.geralanding.imageplanning.service.pending.RecordImagePlanningHypothesis;
import com.marketinghub.geralanding.imageplanning.service.pending.RecordImagePlanningPending;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por adaptar consultas e callbacks para o contrato da etapa image planning. */
@Service
public class BackendImagePlanningService {

    private static final Logger log = LoggerFactory.getLogger(BackendImagePlanningService.class);
    private static final TypeReference<LinkedHashMap<String, Object>> FRAMEWORK_TYPE = new TypeReference<>() {};
    private static final String STAGE_CODE = "landing-page-image-planning";
    private static final String NEXT_STAGE_IMAGE_GENERATION = "landing-page-image-generation";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING_OPENAI_DISPATCH = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** Inicializa o serviço com os repositórios e montadores necessários para a etapa image planning. */
    public BackendImagePlanningService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /** Inicia a execução manual da etapa image planning e publica o reset de imagens antigas antes de gerar novos prompts. */
    @Transactional
    public GeraLandingImagePlanningStartResponse start(Long experimentId) {
        eventPublisher.publishEvent(new ImagePromptRegenerationStartedEvent(experimentId));
        return registerInitialExecution(experimentId, STAGE_CODE);
    }

    /** Evento síncrono que sinaliza a necessidade de zerar imagens antes da regeneração dos prompts. */
    public record ImagePromptRegenerationStartedEvent(Long experimentId) {
    }

    /** Registra a execução inicial da etapa convertendo para o DTO local de início. */
    private GeraLandingImagePlanningStartResponse registerInitialExecution(Long experimentId, String stageCode) {
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
        return new GeraLandingImagePlanningStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
    }

    /** Lista execuções da etapa convertendo para o DTO local da etapa. */
    @Transactional(readOnly = true)
    public List<GeraLandingImagePlanningExecutionSummaryResponse> listExperimentStageExecutions(Long experimentId, String stageCode, boolean includeCompleted) {
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

    /** Lista os jobs iniciados da etapa image planning para processamento independente de experimento. */
    @Transactional(readOnly = true)
    public List<RecordImagePlanningPending> listPending(String stageCode) {
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(stageCode, STATUS_STARTED)
                .stream()
                .map(execution -> new RecordImagePlanningPending(
                        execution.getExperimentId(),
                        fromDatabaseIdJob(execution.getIdJob()),
                        execution.getStageCode(),
                        toPendingExperiment(execution.getExperiment()),
                        toPendingHypothesis(execution.getExperiment())))
                .toList();
    }

    /** Marca a execução como prompt recebido e, quando houver job remoto, aguardando retorno da OpenAI. */
    @Transactional
    public void markPromptReceived(
            String idJob,
            String prompt,
            String promptMarkdownContent,
            String schemaJson,
            String requestBodyJson,
            String openAiModel,
            String openAiJobId) {
        GeraLandingStageExecution execution = findByIdJob(idJob);
        execution.setPrompt(prompt);
        execution.setPromptMarkdownContent(resolvePromptMarkdownContent(prompt, promptMarkdownContent));
        execution.setSchemaJson(schemaJson);
        execution.setOpenAiRequestBody(requestBodyJson);
        execution.setOpenAiModel(openAiModel);
        if (StringUtils.hasText(openAiJobId)) {
            execution.setOpenAiJobId(openAiJobId);
            execution.setStatus(STATUS_WAITING_OPENAI_DISPATCH);
        }
        execution.setProcessingStartedAt(Instant.now());
        executionRepository.save(execution);
    }

    /** Marca a execução como aguardando retorno da OpenAI após receber apenas o identificador remoto. */
    @Transactional
    public void markWaitingOpenAiDispatch(String idJob, String openAiJobId) {
        GeraLandingStageExecution execution = findByIdJob(idJob);
        if (StringUtils.hasText(openAiJobId)) {
            execution.setOpenAiJobId(openAiJobId);
        }
        if (execution.getProcessingStartedAt() == null) {
            execution.setProcessingStartedAt(Instant.now());
        }
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

    /** Conclui ou falha a execução da etapa image planning com a resposta devolvida pelo Worker AI. */
    @Transactional
    public void markCompletedFromResponse(
            String idJob,
            Long experimentId,
            String stageCode,
            String modelResponse,
            String provisionalHtml,
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
            if (normalizedErrorMessage == null) {
                execution.setProvisionalHtml(resolveProvisionalHtml(provisionalHtml));
            }

            executionRepository.save(execution);
            if (normalizedErrorMessage == null) {
                persistImagePlanningArtifactOnExperiment(execution, modelResponse, execution.getProvisionalHtml());
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Erro ao concluir resposta image planning (idJob={}, experimentId={}, stageCode={}, openAiJobId={}, modelResponseLength={}, errorMessage={})",
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

    /** Normaliza a mensagem de erro e garante status FALHA quando o callback traz apenas detalhe técnico. */
    private String normalizeErrorMessage(String errorMessage, String normalizedErrorDetail) {
        if (StringUtils.hasText(errorMessage)) {
            return errorMessage.trim();
        }
        if (StringUtils.hasText(normalizedErrorDetail)) {
            return "Falha ao processar etapa image planning";
        }
        return null;
    }

    /** Usa o HTML provisório recebido pelo callback quando ele faz parte do contrato da resposta. */
    private String resolveProvisionalHtml(String provisionalHtml) {
        return StringUtils.hasText(provisionalHtml) ? provisionalHtml : null;
    }

    /** Persiste o artefato JSON final e o HTML provisório da etapa no experimento associado à execução concluída. */
    private void persistImagePlanningArtifactOnExperiment(GeraLandingStageExecution execution, String modelResponse, String provisionalHtml) {
        if (!StringUtils.hasText(modelResponse) && !StringUtils.hasText(provisionalHtml)) {
            return;
        }
        Experiment experiment = resolveExperiment(execution);
        if (StringUtils.hasText(modelResponse)) {
            experiment.setLandingPageImagePlanning(modelResponse);
        }
        if (StringUtils.hasText(provisionalHtml)) {
            experiment.setLandingPageHtml(provisionalHtml);
        }
        experimentRepository.save(experiment);
        createImageGenerationExecution(experiment);
    }

    /** Agenda a geração de imagens como próxima etapa automática após salvar o planejamento de prompts de imagem. */
    private void createImageGenerationExecution(Experiment experiment) {
        Instant now = Instant.now();
        GeraLandingStageExecution imageGenerationExecution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(NEXT_STAGE_IMAGE_GENERATION)
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId("auto/image-planning")
                .promptContent("Gera Imagem iniciado automaticamente após o Gera Prompt Imagem.")
                .status(STATUS_STARTED)
                .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
                .build();
        executionRepository.save(imageGenerationExecution);
    }

    /** Resolve o experimento da execução, buscando no repositório quando a associação não estiver carregada. */
    private Experiment resolveExperiment(GeraLandingStageExecution execution) {
        Experiment experiment = execution.getExperiment();
        if (experiment == null) {
            experiment = experimentRepository.findById(execution.getExperimentId())
                    .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + execution.getExperimentId()));
        }
        return experiment;
    }

    /** Busca uma execução pelo id_job textual enviado pelos endpoints internos. */
    private GeraLandingStageExecution findByIdJob(String idJob) {
        return executionRepository
                .findTopByIdJobOrderByExecutionRequestedAtDesc(toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException(
                        "GeraLanding execution not found for idJob: " + idJob
                ));
    }

    /** Converte o experimento da execução para os dados expostos na fila pending. */
    private RecordImagePlanningExperiment toPendingExperiment(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordImagePlanningExperiment(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                enumValueToText(experiment.getStatus()),
                enumValueToText(experiment.getStage()),
                experiment.getCreativeTextPrompt(),
                experiment.getCreativeImagePrompt(),
                experiment.getSinglePain(),
                experiment.getFreeReward(),
                experiment.getFunnelPromise(),
                experiment.getPrimaryCta(),
                enumValueToText(experiment.getCampaignObjective()),
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
                    "Falha ao ler artefato JSON no pending image planning; mantendo texto bruto. experimentId={} fieldName={}",
                    experimentId,
                    fieldName,
                    ex);
            return rawValue;
        }
    }

    /** Converte a hipótese associada ao experimento para o framework exposto na fila pending. */
    private RecordImagePlanningHypothesis toPendingHypothesis(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return new RecordImagePlanningHypothesis(
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
                log.warn("Falha ao ler framework da hipótese no pending image planning. experimentId={}", experimentId, ex);
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

    /** Retorna o detalhe da execução convertido para o DTO local da etapa. */
    @Transactional(readOnly = true)
    public RecordBackendImagePlanningDetalheDto getStageExecutionDetail(Long experimentId, String idJob) {
        GeraLandingStageExecution execution = executionRepository
                .findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(experimentId, toDatabaseIdJob(idJob))
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));
        return toDetailResponse(execution);
    }

    /** Converte o resumo transversal para o resumo local da etapa. */
    private GeraLandingImagePlanningExecutionSummaryResponse toSummaryResponse(GeraLandingStageExecution execution) {
        return new GeraLandingImagePlanningExecutionSummaryResponse(
                fromDatabaseIdJob(execution.getIdJob()),
                execution.getStatus(),
                execution.getExecutionRequestedAt(),
                execution.getCostUsd());
    }

    /** Converte o detalhe transversal para o detalhe local da etapa. */
    private RecordBackendImagePlanningDetalheDto toDetailResponse(GeraLandingStageExecution execution) {
        return new RecordBackendImagePlanningDetalheDto(
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
