package com.marketinghub.worker.geralanding;

import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobCompletionPayload;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public GeraLandingExecutionService(GeraLandingBackendClient backendClient,
                                       GeraLandingService geraLandingService,
                                       GeraLandingOpenAiBatchClient openAiClient,
                                       ObjectMapper objectMapper,
                                       @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.backendClient = backendClient;
        this.geraLandingService = geraLandingService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.pendingLimit = Math.max(1, pendingLimit);
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
            return;
        }
        try {
            GeraLandingPromptContext context = new GeraLandingPromptContext(
                    execution.experimentId(),
                    execution.idJob(),
                    execution.stageCode(),
                    java.util.Collections.emptyMap());
            String prompt = geraLandingService.montarERegistrarPromptEtapa(context, normalizedStage);
            log.info("Prompt de gera-landing wireframe montado para executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId());

            String openAiRequestBody = buildOpenAiRequestBody(prompt);
            log.info("Payload OpenAI do gera-landing executionId={}: {}", execution.idJob(), openAiRequestBody);

            ExperimentPipelineJobDto openAiJob = new ExperimentPipelineJobDto(
                    UUID.fromString(execution.idJob()),
                    execution.experimentId(),
                    execution.stageCode(),
                    "gpt-5.2",
                    openAiRequestBody,
                    prompt,
                    null);
            log.info("Enviando gera-landing executionId={} para OpenAI em modo batch lógico", execution.idJob());
            ExperimentPipelineJobCompletionPayload payload = openAiClient.generate(openAiJob);
            backendClient.receiveResult(execution.idJob(), execution.experimentId(), execution.stageCode(), payload);
            log.info("Resultado OpenAI registrado para gera-landing executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId());
        } catch (Exception ex) {
            log.error("Falha ao processar etapa wireframe para executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId(), ex);
        }
    }

    private String buildOpenAiRequestBody(String prompt) throws JsonProcessingException {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", true);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "experiment_pipeline_landing_page_copy");
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "gpt-5.2");
        body.put("input", List.of(
                Map.of("role", "system", "content", "Você é especialista em execução de pipeline de experimento."),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", prompt)))
        ));
        body.put("text", Map.of("format", format));
        return objectMapper.writeValueAsString(body);
    }
}
