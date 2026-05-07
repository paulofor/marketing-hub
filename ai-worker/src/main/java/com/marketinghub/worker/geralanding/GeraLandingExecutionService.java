package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GeraLandingExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingExecutionService.class);
    private static final String STAGE_WIREFRAME = "landing-page-wireframe";

    private final GeraLandingBackendClient backendClient;
    private final GeraLandingService geraLandingService;
    private final GeraLandingOpenAiBatchClient openAiClient;
    private final ObjectMapper objectMapper;
    private final int pendingLimit;
    private final Resource wireframeSchemaResource;

    public GeraLandingExecutionService(GeraLandingBackendClient backendClient,
                                       GeraLandingService geraLandingService,
                                       GeraLandingOpenAiBatchClient openAiClient,
                                       ObjectMapper objectMapper,
                                       @Value("${geralanding.execution.pending-limit:20}") int pendingLimit,
                                       @Value("classpath:prompts/geralanding/landing-page-wireframe-schema.json")
                                       Resource wireframeSchemaResource) {
        this.backendClient = backendClient;
        this.geraLandingService = geraLandingService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.pendingLimit = Math.max(1, pendingLimit);
        this.wireframeSchemaResource = wireframeSchemaResource;
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
        if (!STAGE_WIREFRAME.equals(normalizedStage)) {
            log.info("Skipping gera-landing executionId={} because stageCode {} is not supported (expected {})",
                    execution.idJob(), execution.stageCode(), STAGE_WIREFRAME);
            return;
        }
        try {
            GeraLandingPromptContext context = new GeraLandingPromptContext(
                    execution.experimentId(),
                    execution.idJob(),
                    execution.stageCode(),
                    backendClient.loadPromptData(execution.experimentId()));
            String prompt = geraLandingService.montarERegistrarPromptEtapa(context, normalizedStage);
            String promptMarkdownContent = geraLandingService.carregarPromptMarkdownCru(normalizedStage);
            log.info("Prompt de gera-landing wireframe montado para executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId());

            String openAiRequestBody = buildOpenAiRequestBody(
                    "gpt-5.2",
                    prompt,
                    "gera-landing-pipeline",
                    "Você é especialista em execução de pipeline de experimento.");
            log.info("OpenAI payload built for gera-landing executionId={} (length={})", execution.idJob(), openAiRequestBody.length());
            log.info("Payload OpenAI do gera-landing executionId={}: {}", execution.idJob(), openAiRequestBody);
            String schemaJson = objectMapper.writeValueAsString(readWireframeSchema());
            backendClient.receivePrompt(
                    execution.idJob(),
                    execution.experimentId(),
                    execution.stageCode(),
                    prompt,
                    openAiRequestBody,
                    schemaJson,
                    promptMarkdownContent);

            GeraLandingJobDto openAiJob = new GeraLandingJobDto(
                    UUID.fromString(execution.idJob()),
                    execution.experimentId(),
                    execution.stageCode(),
                    "gpt-5.2",
                    openAiRequestBody,
                    prompt,
                    null);
            log.info("Enviando gera-landing executionId={} para OpenAI em modo batch lógico", execution.idJob());
            GeraLandingJobCompletionPayload payload = openAiClient.generate(openAiJob);
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
            backendClient.receiveResult(execution.idJob(), execution.experimentId(), execution.stageCode(), payload);
            log.info("Resultado OpenAI registrado para gera-landing executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId());
        } catch (Exception ex) {
            log.error("Falha ao processar etapa wireframe para executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId(), ex);
            try {
                backendClient.receiveFailure(execution.idJob(), execution.experimentId(), execution.stageCode(), ex.getMessage());
            } catch (Exception callbackEx) {
                log.error("Falha ao registrar erro de execução no backend para executionId={}", execution.idJob(), callbackEx);
            }
        }
    }

    private String buildOpenAiRequestBody(String model,
                                          String prompt,
                                          String systemName,
                                          String systemMessage) throws JsonProcessingException {
        // Exemplo oficial (OpenAI Responses API) para referência do formato esperado:
        // {
        //   "model": "gpt-5.2",
        //   "input": [
        //     {"role": "system", "content": "Você é especialista em execução de pipeline de experimento."},
        //     {"role": "user", "content": [{"type": "input_text", "text": "SEU PROMPT AQUI"}]}
        //   ],
        //   "text": {
        //     "format": {
        //       "type": "json_schema",
        //       "name": "experiment_pipeline_landing_page_copy",
        //       "schema": {"type": "object", "additionalProperties": true},
        //       "strict": true
        //     }
        //   }
        // }
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "experiment_pipeline_landing_page_copy");
        format.put("schema", readWireframeSchema());
        format.put("strict", true);

        String resolvedModel = StringUtils.hasText(model) ? model.trim() : "gpt-5.2";
        String resolvedSystemName = StringUtils.hasText(systemName) ? systemName.trim() : "system";
        String resolvedSystemMessage = StringUtils.hasText(systemMessage)
                ? systemMessage.trim()
                : "Você é especialista em execução de pipeline de experimento.";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", resolvedModel);
        body.put("input", List.of(
                Map.of("role", "system", "content", "[" + resolvedSystemName + "] " + resolvedSystemMessage),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", prompt)))
        ));
        body.put("text", Map.of("format", format));
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> readWireframeSchema() throws JsonProcessingException {
        try {
            return objectMapper.readValue(wireframeSchemaResource.getInputStream(), Map.class);
        } catch (IOException ex) {
            throw new JsonProcessingException("Falha ao carregar schema do wireframe em classpath:prompts/geralanding/landing-page-wireframe-schema.json") {
            };
        }
    }
}
