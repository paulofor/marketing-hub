package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.stage.GeraLandingStageDefinition;
import com.marketinghub.worker.geralanding.stage.GeraLandingStageSchemaResolver;
import com.marketinghub.worker.geralanding.wireframe.WireframePendingJobsService;
import com.marketinghub.worker.geralanding.copy.CopyPendingJobsService;
import com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService;
import com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService;
import com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService;
import com.marketinghub.worker.geralanding.wireframe.MontaRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Service
/**
 * Executa as etapas pendentes do GeraLanding, preparando prompts, schemas e payloads para a OpenAI.
 */
public class GeraLandingExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingExecutionService.class);
    private static final Pattern BANNED_COPY_TEXT_PATTERN = Pattern.compile(
            "(?i)(adCopy\\.|campaignAngle\\.|landingPageWireframe|uiTags|uiTextTags|copySlots|sectionId|slotId|CASE_DATA|OUTPUT_CONTRACT|template_id|artifact_target|\\bV[1-3]-|lorem ipsum|como funciona \\(passo)");

    private final GeraLandingBackendClient backendClient;
    private final GeraLandingService geraLandingService;
    private final GeraLandingOpenAiFlexClient openAiClient;
    private final ObjectMapper objectMapper;
    private final GeraLandingStageSchemaResolver stageSchemaResolver;
    private final MontaRequest wireframeMontaRequest;
    private final com.marketinghub.worker.geralanding.copy.MontaRequest copyMontaRequest;
    private final com.marketinghub.worker.geralanding.imageplanning.MontaRequest imagePlanningMontaRequest;
    private final com.marketinghub.worker.geralanding.presetdesign.MontaRequest presetDesignMontaRequest;
    private final com.marketinghub.worker.geralanding.deliverables.MontaRequest deliverablesMontaRequest;
    private final com.marketinghub.worker.geralanding.wireframe.RecebeResponse wireframeRecebeResponse;
    private final com.marketinghub.worker.geralanding.copy.RecebeResponse copyRecebeResponse;
    private final com.marketinghub.worker.geralanding.imageplanning.RecebeResponse imagePlanningRecebeResponse;
    private final com.marketinghub.worker.geralanding.presetdesign.RecebeResponse presetDesignRecebeResponse;
    private final com.marketinghub.worker.geralanding.deliverables.RecebeResponse deliverablesRecebeResponse;
    private final WireframePendingJobsService wireframePendingJobsService;
    private final CopyPendingJobsService copyPendingJobsService;
    private final ImagePlanningPendingJobsService imagePlanningPendingJobsService;
    private final PresetDesignPendingJobsService presetDesignPendingJobsService;
    private final DeliverablesPendingJobsService deliverablesPendingJobsService;
    private final int pendingLimit;
    private final Resource wireframeSchemaResource;
    private final Resource copySchemaResource;
    private final Resource imagePlanningSchemaResource;
    private final Resource designPresetSchemaResource;
    private final Resource deliverablesSchemaResource;

    public GeraLandingExecutionService(GeraLandingBackendClient backendClient,
                                       GeraLandingService geraLandingService,
                                       GeraLandingOpenAiFlexClient openAiClient,
                                       ObjectMapper objectMapper,
                                       GeraLandingStageSchemaResolver stageSchemaResolver,
                                       MontaRequest wireframeMontaRequest,
                                       com.marketinghub.worker.geralanding.copy.MontaRequest copyMontaRequest,
                                       com.marketinghub.worker.geralanding.imageplanning.MontaRequest imagePlanningMontaRequest,
                                       com.marketinghub.worker.geralanding.presetdesign.MontaRequest presetDesignMontaRequest,
                                       com.marketinghub.worker.geralanding.deliverables.MontaRequest deliverablesMontaRequest,
                                       com.marketinghub.worker.geralanding.wireframe.RecebeResponse wireframeRecebeResponse,
                                       com.marketinghub.worker.geralanding.copy.RecebeResponse copyRecebeResponse,
                                       com.marketinghub.worker.geralanding.imageplanning.RecebeResponse imagePlanningRecebeResponse,
                                       com.marketinghub.worker.geralanding.presetdesign.RecebeResponse presetDesignRecebeResponse,
                                       com.marketinghub.worker.geralanding.deliverables.RecebeResponse deliverablesRecebeResponse,
                                       WireframePendingJobsService wireframePendingJobsService,
                                       CopyPendingJobsService copyPendingJobsService,
                                       ImagePlanningPendingJobsService imagePlanningPendingJobsService,
                                       PresetDesignPendingJobsService presetDesignPendingJobsService,
                                       DeliverablesPendingJobsService deliverablesPendingJobsService,
                                       @Value("${geralanding.execution.pending-limit:20}") int pendingLimit,
                                       @Value("classpath:prompts/geralanding/landing-page-wireframe-schema.json")
                                       Resource wireframeSchemaResource,
                                       @Value("classpath:prompts/geralanding/landing-page-copy-schema.json")
                                       Resource copySchemaResource,
                                       @Value("classpath:prompts/geralanding/landing-page-image-planning-schema.json")
                                       Resource imagePlanningSchemaResource,
                                       @Value("classpath:prompts/geralanding/landing-page-design-preset-schema.json")
                                       Resource designPresetSchemaResource,
                                       @Value("classpath:prompts/geralanding/landing-page-deliverables-schema.json")
                                       Resource deliverablesSchemaResource) {
        this.backendClient = backendClient;
        this.geraLandingService = geraLandingService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.stageSchemaResolver = stageSchemaResolver;
        this.wireframeMontaRequest = wireframeMontaRequest;
        this.copyMontaRequest = copyMontaRequest;
        this.imagePlanningMontaRequest = imagePlanningMontaRequest;
        this.presetDesignMontaRequest = presetDesignMontaRequest;
        this.deliverablesMontaRequest = deliverablesMontaRequest;
        this.wireframeRecebeResponse = wireframeRecebeResponse;
        this.copyRecebeResponse = copyRecebeResponse;
        this.imagePlanningRecebeResponse = imagePlanningRecebeResponse;
        this.presetDesignRecebeResponse = presetDesignRecebeResponse;
        this.deliverablesRecebeResponse = deliverablesRecebeResponse;
        this.wireframePendingJobsService = wireframePendingJobsService;
        this.copyPendingJobsService = copyPendingJobsService;
        this.imagePlanningPendingJobsService = imagePlanningPendingJobsService;
        this.presetDesignPendingJobsService = presetDesignPendingJobsService;
        this.deliverablesPendingJobsService = deliverablesPendingJobsService;
        this.pendingLimit = Math.max(1, pendingLimit);
        this.wireframeSchemaResource = wireframeSchemaResource;
        this.copySchemaResource = copySchemaResource;
        this.imagePlanningSchemaResource = imagePlanningSchemaResource;
        this.designPresetSchemaResource = designPresetSchemaResource;
        this.deliverablesSchemaResource = deliverablesSchemaResource;
    }

    public void processPendingExecutions() {
        if (!openAiClient.isEnabled()) {
            log.warn("GeraLanding generation skipped: OpenAI client is disabled");
            return;
        }
        List<GeraLandingStageExecutionDto> pending = backendClient.listPendingExecutions(pendingLimit);
        List<GeraLandingStageExecutionDto> wireframePending = wireframePendingJobsService.listPendingWireframeJobs(pendingLimit);
        List<com.marketinghub.worker.geralanding.copy.GeraLandingStageExecutionDto> copyPending = copyPendingJobsService.listPendingCopyJobs(pendingLimit);
        List<GeraLandingStageExecutionDto> imagePlanningPending = imagePlanningPendingJobsService.listPendingImagePlanningJobs(pendingLimit);
        List<GeraLandingStageExecutionDto> presetDesignPending = presetDesignPendingJobsService.listPendingPresetDesignJobs(pendingLimit);
        List<GeraLandingStageExecutionDto> deliverablesPending = deliverablesPendingJobsService.listPendingDeliverablesJobs(pendingLimit);
        log.info("Stage pending jobs via stage controllers: wireframe={}, copy={}, imagePlanning={}, designPreset={}, deliverables={}",
                wireframePending.size(), copyPending.size(), imagePlanningPending.size(), presetDesignPending.size(), deliverablesPending.size());
        log.info("GeraLanding execution worker found {} pending execution(s)", pending.size());
        processExecutions(pending);
    }

    /**
     * Processa uma lista de execuções já filtradas por uma etapa específica.
     */
    public void processExecutions(List<GeraLandingStageExecutionDto> executions) {
        if (!openAiClient.isEnabled()) {
            log.warn("GeraLanding generation skipped: OpenAI client is disabled");
            return;
        }
        for (GeraLandingStageExecutionDto execution : executions) {
            processExecution(execution);
        }
    }

    private void processExecution(GeraLandingStageExecutionDto execution) {
        if (execution == null || !StringUtils.hasText(execution.stageCode()) || !StringUtils.hasText(execution.idJob())) {
            log.warn("Skipping gera-landing execution with missing required fields. experimentId={}, stageCode={}, idJob={}",
                    execution != null ? execution.experimentId() : null,
                    execution != null ? execution.stageCode() : null,
                    execution != null ? execution.idJob() : null);
            return;
        }
        String normalizedStage = execution.stageCode().trim().toLowerCase(Locale.ROOT);
        GeraLandingStageDefinition stage = GeraLandingStageDefinition.fromCode(normalizedStage);
        if (stage == null) {
            log.info("Skipping gera-landing executionId={} because stageCode {} is not supported",
                    execution.idJob(), execution.stageCode());
            return;
        }
        try {
            final String openAiModel = "gpt-5.2";
            Map<String, Object> dadosPrompt = backendClient.loadPromptData(execution.experimentId());
            GeraLandingExperimentRequest requestData = new GeraLandingExperimentRequest(execution.experimentId(), dadosPrompt);
            String prompt = montarPromptPorEtapa(stage, requestData);
            String promptMarkdownContent = carregarPromptMarkdownPorEtapa(stage);
            log.info("Prompt de gera-landing da etapa {} montado para executionId={} (experimentId={})",
                    execution.stageCode(), execution.idJob(), execution.experimentId());

            String openAiRequestBody = montarRequestPorEtapa(
                    stage,
                    requestData);
            log.info("OpenAI payload built for gera-landing executionId={} (length={})", execution.idJob(), openAiRequestBody.length());
            log.info("Payload OpenAI do gera-landing executionId={}: {}", execution.idJob(), openAiRequestBody);
            String schemaJson = objectMapper.writeValueAsString(readSchemaByStage(stage));
            backendClient.receivePrompt(
                    execution.idJob(),
                    execution.experimentId(),
                    execution.stageCode(),
                    prompt,
                    openAiRequestBody,
                    openAiModel,
                    schemaJson,
                    promptMarkdownContent);

            GeraLandingJobDto openAiJob = new GeraLandingJobDto(
                    UUID.fromString(execution.idJob()),
                    execution.experimentId(),
                    execution.stageCode(),
                    openAiModel,
                    openAiRequestBody,
                    prompt,
                    null);
            log.info("Enviando gera-landing executionId={} para OpenAI em modo flex", execution.idJob());
            GeraLandingJobCompletionPayload payload = openAiClient.generate(openAiJob);
            validateWireframeStyleReferences(stage, payload);
            validateDesignPresetStyleReferences(stage, payload);
            validateCopyPayloadText(stage, payload);
            log.info(
                    "Resposta OpenAI recebida para gera-landing executionId={} (experimentId={}, openAiJobId={}, inputTokens={}, outputTokens={}, costUsd={}, responseContentLength={}, rawResponseLength={})",
                    execution.idJob(),
                    execution.experimentId(),
                    payload != null ? payload.openAiJobId() : null,
                    payload != null ? payload.inputTokens() : null,
                    payload != null ? payload.outputTokens() : null,
                    payload != null ? payload.costUsd() : null,
                    payload != null && payload.responseContent() != null ? payload.responseContent().length() : null,
                    payload != null && payload.rawResponse() != null ? payload.rawResponse().length() : null);
            if (payload != null && StringUtils.hasText(payload.responseContent())) {
                log.info("Resposta OpenAI (responseContent) executionId={}: {}", execution.idJob(), payload.responseContent());
            }
            encaminharRespostaDaEtapa(stage, execution, payload);
            log.info("Resultado OpenAI registrado para gera-landing executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId());
        } catch (Exception ex) {
            log.error("Falha ao processar etapa {} para executionId={} (experimentId={})",
                    execution.stageCode(), execution.idJob(), execution.experimentId(), ex);
            try {
                backendClient.receiveFailure(
                        execution.idJob(),
                        execution.experimentId(),
                        execution.stageCode(),
                        ex.getMessage(),
                        ExceptionUtils.getRootCauseMessage(ex));
            } catch (Exception callbackEx) {
                log.error("Falha ao registrar erro de execução no backend para executionId={}", execution.idJob(), callbackEx);
            }
        }
    }

    /**
     * Encaminha a resposta da OpenAI para o processador da etapa, que monta e envia o payload ao backend.
     */
    private void encaminharRespostaDaEtapa(GeraLandingStageDefinition stage,
                                           GeraLandingStageExecutionDto execution,
                                           GeraLandingJobCompletionPayload payload) {
        switch (stage) {
            case WIREFRAME -> wireframeRecebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
            case COPY -> copyRecebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), toCopyPayload(payload));
            case IMAGE_PLANNING -> imagePlanningRecebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
            case DESIGN_PRESET -> presetDesignRecebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
            case DELIVERABLES -> deliverablesRecebeResponse.processar(execution.experimentId(), execution.stageCode(), execution.idJob(), payload);
            default -> throw new IllegalStateException("Etapa não suportada para processamento de resposta: " + stage);
        }
    }

    private void validateCopyPayloadText(GeraLandingStageDefinition stage, GeraLandingJobCompletionPayload payload) {
        if (stage != GeraLandingStageDefinition.COPY || payload == null || !StringUtils.hasText(payload.responseContent())) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload.responseContent());
            List<String> violations = new ArrayList<>();
            collectViolations(root.path("hero"), "hero", violations);
            collectViolations(root.path("bodySections"), "bodySections", violations);
            collectViolations(root.path("ctaBlocks"), "ctaBlocks", violations);
            collectViolations(root.path("faq"), "faq", violations);
            if (!violations.isEmpty()) {
                throw new IllegalStateException("Copy inválida: vazamento de metainstrução/texto técnico detectado em "
                        + String.join("; ", violations));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Copy inválida: resposta não é JSON parseável para validação textual", ex);
        }
    }

    /**
     * Valida se o preset design referencia em pagina.estilos somente classes definidas em definicoes.
     */
    private void validateDesignPresetStyleReferences(GeraLandingStageDefinition stage, GeraLandingJobCompletionPayload payload) {
        if (stage != GeraLandingStageDefinition.DESIGN_PRESET || payload == null || !StringUtils.hasText(payload.responseContent())) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload.responseContent());
            Set<String> allowedStyleNames = collectAllowedStyleNamesFromDesignPreset(root.path("definicoes"));
            if (allowedStyleNames.isEmpty()) {
                throw new IllegalStateException("Preset design inválido: bloco 'definicoes' sem estilos válidos para referência.");
            }
            List<String> violations = new ArrayList<>();
            collectInvalidStyleReferenceList(root.path("pagina").path("body").path("estilos"), "pagina.body.estilos", allowedStyleNames, violations);
            collectInvalidStyleReferenceList(root.path("pagina").path("corpo").path("estilos"), "pagina.corpo.estilos", allowedStyleNames, violations);
            collectInvalidDesignPresetNodeStyles(root.path("pagina").path("corpo").path("secoes"), "pagina.corpo.secoes", allowedStyleNames, violations);
            if (!violations.isEmpty()) {
                throw new IllegalStateException("Preset design inválido: estilos inexistentes nas definições canônicas. " + String.join("; ", violations));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Preset design inválido: resposta não é JSON parseável para validação de estilos", ex);
        }
    }

    /**
     * Coleta nomes de estilos permitidos de todos os grupos de definicoes da etapa preset design.
     */
    private Set<String> collectAllowedStyleNamesFromDesignPreset(JsonNode definicoesNode) {
        Set<String> names = new HashSet<>();
        if (!definicoesNode.isObject()) {
            return names;
        }
        definicoesNode.fieldNames().forEachRemaining(groupName ->
                collectStyleNamesFromDeviceBucket(definicoesNode.path(groupName), names));
        return names;
    }

    /**
     * Valida estilos em secoes e elementos do preset design recursivamente.
     */
    private void collectInvalidDesignPresetNodeStyles(JsonNode nodes, String path, Set<String> allowedStyleNames, List<String> violations) {
        if (!nodes.isArray()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            String nodePath = path + "[" + i + "]";
            collectInvalidStyleReferenceList(node.path("estilos"), nodePath + ".estilos", allowedStyleNames, violations);
            collectInvalidDesignPresetNodeStyles(node.path("elementosSeccao"), nodePath + ".elementosSeccao", allowedStyleNames, violations);
            collectInvalidDesignPresetNodeStyles(node.path("elementosInternos"), nodePath + ".elementosInternos", allowedStyleNames, violations);
        }
    }

    /**
     * Valida se os estilos referenciados no wireframe existem no bloco de definições do próprio artefato.
     */
    private void validateWireframeStyleReferences(GeraLandingStageDefinition stage, GeraLandingJobCompletionPayload payload) {
        if (stage != GeraLandingStageDefinition.WIREFRAME || payload == null || !StringUtils.hasText(payload.responseContent())) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload.responseContent());
            Set<String> allowedStyleNames = collectAllowedStyleNames(root.path("definicoes"));
            if (allowedStyleNames.isEmpty()) {
                throw new IllegalStateException("Wireframe inválido: bloco 'definicoes' sem estilos válidos para referência.");
            }
            List<String> violations = new ArrayList<>();
            collectInvalidStyleReferences(root.path("pagina").path("corpo").path("secoes"), allowedStyleNames, violations);
            if (!violations.isEmpty()) {
                throw new IllegalStateException("Wireframe inválido: estilos inexistentes nas definições canônicas. " + String.join("; ", violations));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Wireframe inválido: resposta não é JSON parseável para validação de estilos", ex);
        }
    }

    /**
     * Coleta todos os nomes de estilos permitidos por categoria e dispositivo.
     */
    private Set<String> collectAllowedStyleNames(JsonNode definicoesNode) {
        Set<String> names = new HashSet<>();
        collectStyleNamesFromDeviceBucket(definicoesNode.path("estrutura"), names);
        collectStyleNamesFromDeviceBucket(definicoesNode.path("posicao"), names);
        collectStyleNamesFromDeviceBucket(definicoesNode.path("layout"), names);
        collectStyleNamesFromDeviceBucket(definicoesNode.path("mistas"), names);
        return names;
    }

    /**
     * Extrai nomes de estilos para desktop e mobile de uma categoria de definições.
     */
    private void collectStyleNamesFromDeviceBucket(JsonNode categoryNode, Set<String> collector) {
        collectStyleNames(categoryNode.path("desktop"), collector);
        collectStyleNames(categoryNode.path("mobile"), collector);
    }

    /**
     * Adiciona ao coletor os nomes válidos encontrados na lista de definições.
     */
    private void collectStyleNames(JsonNode definitionsNode, Set<String> collector) {
        if (!definitionsNode.isArray()) {
            return;
        }
        for (JsonNode definition : definitionsNode) {
            JsonNode nameNode = definition.path("nome");
            if (nameNode.isTextual() && StringUtils.hasText(nameNode.asText())) {
                collector.add(nameNode.asText());
            }
        }
    }

    /**
     * Verifica estilos inválidos no nível das seções e delega a validação recursiva dos elementos.
     */
    private void collectInvalidStyleReferences(JsonNode secoesNode, Set<String> allowedStyleNames, List<String> violations) {
        if (!secoesNode.isArray()) {
            return;
        }
        for (int i = 0; i < secoesNode.size(); i++) {
            JsonNode secaoNode = secoesNode.get(i);
            collectInvalidStyleReferenceList(secaoNode.path("estilos"), "pagina.corpo.secoes[" + i + "].estilos", allowedStyleNames, violations);
            collectInvalidElementStyles(secaoNode.path("elementosSeccao"), "pagina.corpo.secoes[" + i + "].elementosSeccao", allowedStyleNames, violations);
        }
    }

    /**
     * Valida de forma recursiva estilos inválidos em elementos e elementos internos.
     */
    private void collectInvalidElementStyles(JsonNode elementsNode, String path, Set<String> allowedStyleNames, List<String> violations) {
        if (!elementsNode.isArray()) {
            return;
        }
        for (int i = 0; i < elementsNode.size(); i++) {
            JsonNode elementNode = elementsNode.get(i);
            String elementPath = path + "[" + i + "]";
            collectInvalidStyleReferenceList(elementNode.path("estilos"), elementPath + ".estilos", allowedStyleNames, violations);
            collectInvalidElementStyles(elementNode.path("elementosInternos"), elementPath + ".elementosInternos", allowedStyleNames, violations);
        }
    }

    /**
     * Registra violações para cada referência de estilo ausente no conjunto permitido.
     */
    private void collectInvalidStyleReferenceList(JsonNode styleRefsNode, String path, Set<String> allowedStyleNames, List<String> violations) {
        if (!styleRefsNode.isArray()) {
            return;
        }
        for (int i = 0; i < styleRefsNode.size(); i++) {
            JsonNode refNode = styleRefsNode.get(i);
            if (!refNode.isTextual()) {
                continue;
            }
            String styleRef = refNode.asText();
            if (!allowedStyleNames.contains(styleRef)) {
                violations.add(path + "[" + i + "]='" + styleRef + "'");
            }
        }
    }

    private void collectViolations(JsonNode node, String path, List<String> violations) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            String value = node.asText();
            if (BANNED_COPY_TEXT_PATTERN.matcher(value).find()) {
                violations.add(path + "='" + abbreviate(value, 100) + "'");
            }
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectViolations(node.get(i), path + "[" + i + "]", violations);
            }
            return;
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(field -> collectViolations(node.get(field), path + "." + field, violations));
        }
    }

    private String abbreviate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

    /**
     * Direciona a montagem do request da OpenAI para o montador específico de cada etapa.
     */
    private String montarRequestPorEtapa(GeraLandingStageDefinition stage,
                                         GeraLandingExperimentRequest experiment) throws IOException {
        return switch (stage) {
            case WIREFRAME -> wireframeMontaRequest.montar(experiment);
            case COPY -> copyMontaRequest.montar(toCopyExperiment(experiment));
            case IMAGE_PLANNING -> imagePlanningMontaRequest.montar(experiment);
            case DESIGN_PRESET -> presetDesignMontaRequest.montar(experiment);
            case DELIVERABLES -> deliverablesMontaRequest.montar(experiment);
        };
    }


    /** Direciona a montagem do prompt para o montador específico de cada etapa. */
    private String montarPromptPorEtapa(GeraLandingStageDefinition stage,
                                        GeraLandingExperimentRequest experiment) throws IOException {
        return switch (stage) {
            case WIREFRAME -> wireframeMontaRequest.montarPrompt(experiment);
            case COPY -> copyMontaRequest.montarPrompt(toCopyExperiment(experiment));
            case IMAGE_PLANNING -> imagePlanningMontaRequest.montarPrompt(experiment);
            case DESIGN_PRESET -> presetDesignMontaRequest.montarPrompt(experiment);
            case DELIVERABLES -> deliverablesMontaRequest.montarPrompt(experiment);
        };
    }

    /** Direciona o carregamento do markdown bruto para o montador específico de cada etapa. */
    private String carregarPromptMarkdownPorEtapa(GeraLandingStageDefinition stage) throws IOException {
        return switch (stage) {
            case WIREFRAME -> wireframeMontaRequest.carregarPromptMarkdownCru();
            case COPY -> copyMontaRequest.carregarPromptMarkdownCru();
            case IMAGE_PLANNING -> imagePlanningMontaRequest.carregarPromptMarkdownCru();
            case DESIGN_PRESET -> presetDesignMontaRequest.carregarPromptMarkdownCru();
            case DELIVERABLES -> deliverablesMontaRequest.carregarPromptMarkdownCru();
        };
    }

    /** Converte o request comum para o tipo isolado da etapa copy. */
    private com.marketinghub.worker.geralanding.copy.GeraLandingExperimentRequest toCopyExperiment(GeraLandingExperimentRequest experiment) {
        return new com.marketinghub.worker.geralanding.copy.GeraLandingExperimentRequest(experiment.experimentId(), experiment.dados());
    }

    /** Converte o payload comum para o tipo isolado da etapa copy. */
    private com.marketinghub.worker.geralanding.copy.GeraLandingJobCompletionPayload toCopyPayload(GeraLandingJobCompletionPayload payload) {
        if (payload == null) {
            return null;
        }
        return new com.marketinghub.worker.geralanding.copy.GeraLandingJobCompletionPayload(
                payload.responseContent(),
                payload.rawResponse(),
                payload.requestBodyJson(),
                payload.openAiJobId(),
                payload.inputTokens(),
                payload.outputTokens(),
                payload.costUsd());
    }

    private Map<String, Object> readSchemaByStage(GeraLandingStageDefinition stage) throws JsonProcessingException {
        return stageSchemaResolver.resolveSchema(
                stage,
                wireframeSchemaResource,
                copySchemaResource,
                imagePlanningSchemaResource,
                designPresetSchemaResource,
                deliverablesSchemaResource);
    }
}
