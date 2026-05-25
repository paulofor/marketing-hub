package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.stage.GeraLandingStageDefinition;
import com.marketinghub.worker.geralanding.stage.GeraLandingStageSchemaResolver;
import com.marketinghub.worker.geralanding.wireframe.MontaRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        log.info("GeraLanding execution worker found {} pending execution(s)", pending.size());
        for (GeraLandingStageExecutionDto execution : pending) {
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
            GeraLandingPromptContext context = new GeraLandingPromptContext(
                    execution.experimentId(),
                    execution.idJob(),
                    execution.stageCode(),
                    backendClient.loadPromptData(execution.experimentId()));
            String prompt = geraLandingService.montarERegistrarPromptEtapa(context, normalizedStage);
            String promptMarkdownContent = geraLandingService.carregarPromptMarkdownCru(normalizedStage);
            log.info("Prompt de gera-landing da etapa {} montado para executionId={} (experimentId={})",
                    execution.stageCode(), execution.idJob(), execution.experimentId());

            String openAiRequestBody = montarRequestPorEtapa(
                    stage,
                    openAiModel,
                    prompt,
                    "gera-landing-pipeline",
                    "Você é um Especialista em Marketing focado em vendas de produtos digitais pela Internet.");
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
            if (payload != null && StringUtils.hasText(payload.openAiJobId())) {
                backendClient.receiveDispatch(execution.idJob(), execution.experimentId(), execution.stageCode(), payload.openAiJobId());
            }
            backendClient.receiveResult(execution.idJob(), execution.experimentId(), execution.stageCode(), payload);
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
                                         String model,
                                         String prompt,
                                         String systemName,
                                         String systemMessage) throws JsonProcessingException {
        Map<String, Object> schema = readSchemaByStage(stage);
        return switch (stage) {
            case WIREFRAME -> wireframeMontaRequest.montar(model, prompt, systemName, systemMessage, schema);
            case COPY -> copyMontaRequest.montar(model, prompt, systemName, systemMessage, schema);
            case IMAGE_PLANNING -> imagePlanningMontaRequest.montar(model, prompt, systemName, systemMessage, schema);
            case DESIGN_PRESET -> presetDesignMontaRequest.montar(model, prompt, systemName, systemMessage, schema);
            case DELIVERABLES -> deliverablesMontaRequest.montar(model, prompt, systemName, systemMessage, schema);
        };
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
