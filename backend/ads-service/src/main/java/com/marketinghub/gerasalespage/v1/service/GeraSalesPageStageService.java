package com.marketinghub.gerasalespage.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePromptSchemaTemplate;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: controlar fila, auditoria e avanço das etapas do GeraSalesPage v1. */
@Service
public class GeraSalesPageStageService {
    private static final Logger log = LoggerFactory.getLogger(GeraSalesPageStageService.class);
    private static final String PIPELINE_CODE = "gera-sales-page-v1";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_PROCESSING = "EM_PROCESSAMENTO";
    private static final String STATUS_WAITING_OPENAI = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private static final String STATUS_REPLACED = "SUBSTITUIDO";

    private final ExperimentRepository experimentRepository;
    private final GeraSalesPageStageExecutionRepository executionRepository;
    private final GeraSalesPagePromptSchemaTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com repositórios e serializador usados pelos contratos internos. */
    public GeraSalesPageStageService(
            ExperimentRepository experimentRepository,
            GeraSalesPageStageExecutionRepository executionRepository,
            GeraSalesPagePromptSchemaTemplateRepository templateRepository,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    /** Inicia o pipeline na primeira etapa, bloqueando experimento sem checkout real. */
    @Transactional
    public GeraSalesPageStartResponse start(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        validateCheckoutUrl(experiment);
        GeraSalesPageStageExecution execution = enqueue(experimentId, GeraSalesPageStageCode.OFFER_BRIEF.code());
        return new GeraSalesPageStartResponse(experimentId, execution.getStageCode(), idJobText(execution), execution.getStatus());
    }

    /** Substitui execuções anteriores e reinicia o GeraSalesPage v1 desde a primeira etapa. */
    @Transactional
    public GeraSalesPageStartResponse rebuild(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        validateCheckoutUrl(experiment);
        List<GeraSalesPageStageExecution> executions =
                executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(experimentId);
        executions.forEach(execution -> {
            if (!STATUS_REPLACED.equalsIgnoreCase(execution.getStatus())) {
                execution.setStatus(STATUS_REPLACED);
                execution.setErrorMessage("Execução substituída por rebuild manual do GeraSalesPage v1.");
            }
        });
        executionRepository.saveAll(executions);
        GeraSalesPageStageExecution execution = createNewExecution(experimentId, GeraSalesPageStageCode.OFFER_BRIEF.code());
        return new GeraSalesPageStartResponse(experimentId, execution.getStageCode(), idJobText(execution), execution.getStatus());
    }

    /** Lista pendências de uma etapa e inclui prompt/schema ativo do banco no contrato do worker. */
    @Transactional(readOnly = true)
    public List<GeraSalesPagePendingResponse> pending(String stageCode) {
        validateStage(stageCode);
        GeraSalesPagePromptSchemaTemplate template = loadTemplate(stageCode);
        return executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(stageCode, STATUS_STARTED)
                .stream()
                .map(execution -> toPendingResponse(execution, template))
                .toList();
    }

    /** Marca uma execução como em processamento para evitar captura concorrente pelo worker. */
    @Transactional
    public void markRunning(String idJob) {
        GeraSalesPageStageExecution execution = findExecution(idJob);
        execution.setStatus(STATUS_PROCESSING);
        execution.setProcessingStartedAt(Instant.now());
        executionRepository.save(execution);
    }

    /** Salva prompt, schema e request bruto enviados pelo worker à OpenAI. */
    @Transactional
    public void receivePrompt(String idJob, GeraSalesPagePromptRequest payload) {
        GeraSalesPageStageExecution execution = findExecution(idJob);
        execution.setStatus(STATUS_WAITING_OPENAI);
        execution.setPrompt(payload.prompt());
        execution.setPromptMarkdownContent(payload.promptMarkdownContent());
        execution.setSchemaJson(payload.schemaJson());
        execution.setOpenAiRequestBody(payload.requestBodyJson());
        execution.setOpenAiModel(payload.openAiModel());
        execution.setOpenAiJobId(payload.openAiJobId());
        executionRepository.save(execution);
    }

    /** Salva resposta ou falha da etapa e enfileira a próxima etapa quando houver sucesso. */
    @Transactional
    public void receiveResult(String idJob, GeraSalesPageResultRequest payload) {
        GeraSalesPageStageExecution execution = findExecution(idJob);
        if (StringUtils.hasText(payload.errorMessage())) {
            execution.setStatus(STATUS_FAILED);
            execution.setErrorMessage(payload.errorMessage());
            execution.setErrorDetail(payload.errorDetail());
        } else {
            execution.setStatus(STATUS_COMPLETED);
            execution.setModelResponse(payload.modelResponse());
            execution.setRawResponse(payload.rawResponse());
            execution.setInputTokens(payload.inputTokens());
            execution.setOutputTokens(payload.outputTokens());
            execution.setCostUsd(payload.costUsd());
            execution.setOpenAiJobId(payload.openAiJobId());
            execution.setCompletedAt(Instant.now());
            GeraSalesPageStageCode.nextAfter(execution.getStageCode())
                    .ifPresent(nextStage -> enqueue(execution.getExperimentId(), nextStage));
        }
        executionRepository.save(execution);
    }

    /** Enfileira uma etapa se ela ainda não estiver concluída ou pendente no experimento. */
    private GeraSalesPageStageExecution enqueue(Long experimentId, String stageCode) {
        return executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode)
                .filter(existing -> !STATUS_FAILED.equals(existing.getStatus()))
                .filter(existing -> !STATUS_REPLACED.equals(existing.getStatus()))
                .orElseGet(() -> {
                    return createNewExecution(experimentId, stageCode);
                });
    }

    /** Cria uma nova execução de etapa com template ativo e idJob único. */
    private GeraSalesPageStageExecution createNewExecution(Long experimentId, String stageCode) {
        GeraSalesPagePromptSchemaTemplate template = loadTemplate(stageCode);
        GeraSalesPageStageExecution execution = GeraSalesPageStageExecution.builder()
                .idJob(UUID.randomUUID().toString())
                .experimentId(experimentId)
                .stageCode(stageCode)
                .status(STATUS_STARTED)
                .executionRequestedAt(Instant.now())
                .promptTemplateKey(template.getTemplateKey())
                .build();
        return executionRepository.save(execution);
    }

    /** Converte uma execução persistida no payload de pendência consumido pelo worker. */
    private GeraSalesPagePendingResponse toPendingResponse(
            GeraSalesPageStageExecution execution,
            GeraSalesPagePromptSchemaTemplate template) {
        Experiment experiment = execution.getExperiment();
        return new GeraSalesPagePendingResponse(
                execution.getExperimentId(),
                execution.getStageCode(),
                idJobText(execution),
                execution.getExecutionRequestedAt(),
                experimentPayload(experiment),
                templatePayload(template),
                previousStageOutputs(execution.getExperimentId()));
    }

    /** Monta o contexto comercial mínimo do experimento para os prompts da página de vendas. */
    private Map<String, Object> experimentPayload(Experiment experiment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", experiment.getId());
        payload.put("name", experiment.getName());
        payload.put("nicheName", experiment.getNiche() != null ? experiment.getNiche().getName() : "");
        payload.put("hypothesis", experiment.getHypothesis());
        payload.put("singlePain", experiment.getSinglePain());
        payload.put("freeReward", experiment.getFreeReward());
        payload.put("funnelPromise", experiment.getFunnelPromise());
        payload.put("primaryCta", experiment.getPrimaryCta());
        payload.put("campaignObjective", experiment.getCampaignObjective());
        payload.put("campaignAngle", parseJsonOrText(experiment.getCampaignAngle()));
        payload.put("adCopy", parseJsonOrText(experiment.getAdCopy()));
        payload.put("adImageBriefing", parseJsonOrText(experiment.getAdImageBriefing()));
        payload.put("checkoutUrl", experiment.getFollowUpActionUrl());
        payload.put("unitPrice", experiment.getUnitPrice());
        payload.put("hypothesisFramework", parseJsonOrText(experiment.getHypothesisFrameworkJsonForPending()));
        return payload;
    }

    /** Monta o bloco de template ativo entregue ao worker. */
    private Map<String, Object> templatePayload(GeraSalesPagePromptSchemaTemplate template) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateKey", template.getTemplateKey());
        payload.put("version", template.getVersion());
        payload.put("model", template.getOpenAiModel());
        payload.put("schemaName", template.getSchemaName());
        payload.put("promptMarkdownContent", template.getPromptMarkdownContent());
        payload.put("schemaJson", template.getSchemaJson());
        return payload;
    }

    /** Lê as saídas anteriores concluídas para encadear contexto sem acoplamento entre etapas no worker. */
    private Map<String, Object> previousStageOutputs(Long experimentId) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        for (String stage : GeraSalesPageStageCode.orderedCodes()) {
            executionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stage)
                    .filter(execution -> STATUS_COMPLETED.equals(execution.getStatus()))
                    .ifPresent(execution -> outputs.put(stage, parseJsonOrText(execution.getModelResponse())));
        }
        return outputs;
    }

    /** Carrega o template ativo da etapa e falha cedo se ele não existir. */
    private GeraSalesPagePromptSchemaTemplate loadTemplate(String stageCode) {
        return templateRepository.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(PIPELINE_CODE, stageCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Template ativo de prompt/schema não encontrado para " + stageCode));
    }

    /** Busca uma execução pelo idJob textual usado nos endpoints. */
    private GeraSalesPageStageExecution findExecution(String idJob) {
        return executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(idJob)
                .orElseThrow(() -> new EntityNotFoundException("GeraSalesPage job not found: " + idJob));
    }

    /** Valida se a etapa pertence ao GeraSalesPage v1. */
    private void validateStage(String stageCode) {
        if (!GeraSalesPageStageCode.contains(stageCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa GeraSalesPage v1 desconhecida: " + stageCode);
        }
    }

    /** Verifica URL de checkout real e bloqueia âncora local ou URL vazia. */
    private boolean isRealCheckoutUrl(String url) {
        return StringUtils.hasText(url)
                && (url.startsWith("http://") || url.startsWith("https://"))
                && !url.contains("#checkout");
    }

    /** Bloqueia início ou rebuild sem checkout real persistido no experimento. */
    private void validateCheckoutUrl(Experiment experiment) {
        if (!isRealCheckoutUrl(experiment.getFollowUpActionUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "GeraSalesPage v1 exige followUpActionUrl com URL real de checkout antes de iniciar.");
        }
    }

    /** Converte JSON textual quando possível, preservando texto simples quando não for JSON. */
    private Object parseJsonOrText(String value) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException ex) {
            log.debug("Campo textual do GeraSalesPage não é JSON válido; preservando texto.", ex);
            return value;
        }
    }

    /** Converte o idJob binário no texto original salvo pela aplicação. */
    private String idJobText(GeraSalesPageStageExecution execution) {
        return execution.getIdJob();
    }
}
