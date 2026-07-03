package com.marketinghub.worker.pipeline.hypothesismechanism;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.openai.core.prompt.PromptTemplateResolver;
import com.marketinghub.worker.pipeline.BackendPromptSchemaTemplate;
import com.marketinghub.worker.pipeline.StageArtifact;
import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: construir o mecanismo do nicho usando uma etapa OpenAI isolada no pipeline genérico. */
public class HypothesisMechanismProcessor implements StageProcessor<HypothesisMechanismInput, HypothesisMechanismOutput> {
    private static final Logger log = LoggerFactory.getLogger(HypothesisMechanismProcessor.class);
    private final ObjectMapper objectMapper;
    private final HypothesisMechanismWorkerProperties properties;
    private final OpenAiClientPort openAiClient;
    private final HypothesisMechanismResponseValidator responseValidator;
    private final HypothesisMechanismBackendClient backendClient;
    private final PromptTemplateResolver promptTemplateResolver;

    /** Inicializa o processor com dependências específicas da etapa Mecanismo. */
    public HypothesisMechanismProcessor(
            ObjectMapper objectMapper,
            HypothesisMechanismWorkerProperties properties,
            OpenAiClientPort openAiClient,
            HypothesisMechanismResponseValidator responseValidator,
            HypothesisMechanismBackendClient backendClient) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.openAiClient = openAiClient;
        this.responseValidator = responseValidator;
        this.backendClient = backendClient;
        this.promptTemplateResolver = new PromptTemplateResolver(this::loadResource, this::toJsonOrText);
    }

    /** Processa a etapa criando prompt, enviando para OpenAI, validando JSON e registrando artefatos. */
    @Override
    public StageResult<HypothesisMechanismOutput> process(StageContext<HypothesisMechanismInput> context) {
        OpenAiRequest request = buildOpenAiRequest(context);
        backendClient.saveOpenAiRequest(context.execution(), request);
        log.info("Enviando request cru para OpenAI na etapa Mecanismo. jobId={} requestBodyJson={}", context.execution().idJob(), request.requestBodyJson());
        OpenAiDispatch dispatch = openAiClient.dispatch(request);
        OpenAiResult<String> rawResult = openAiClient.awaitResult(dispatch);
        log.info("Resposta crua da OpenAI recebida na etapa Mecanismo. jobId={} rawResponse={}", context.execution().idJob(), rawResult.rawResponse());
        HypothesisMechanismOutput output = responseValidator.validateAndParse(rawResult.modelResponse());
        StageArtifact requestArtifact = context.artifactStore().save(
                "OPENAI_REQUEST",
                "hypothesis-mechanism-request.json",
                "application/json",
                request.requestBodyJson(),
                Map.of("idJob", context.execution().idJob(), "stageCode", context.execution().stageCode()));
        StageArtifact responseArtifact = context.artifactStore().save(
                "OPENAI_RESPONSE",
                "hypothesis-mechanism-response.json",
                "application/json",
                rawResult.rawResponse(),
                Map.of("idJob", context.execution().idJob(), "stageCode", context.execution().stageCode()));
        StageArtifact outputArtifact = context.artifactStore().save(
                "NORMALIZED_JSON",
                "hypothesis-mechanism-output.json",
                "application/json",
                rawResult.modelResponse(),
                Map.of("idJob", context.execution().idJob(), "stageCode", context.execution().stageCode()));
        return new StageResult<>(
                output,
                List.of(requestArtifact, responseArtifact, outputArtifact),
                Map.of(
                        "openAiResult", rawResult,
                        "openAiJobId", rawResult.openAiJobId() == null ? "" : rawResult.openAiJobId(),
                        "inputTokens", rawResult.inputTokens() == null ? 0 : rawResult.inputTokens(),
                        "outputTokens", rawResult.outputTokens() == null ? 0 : rawResult.outputTokens()));
    }

    /** Monta o request completo da OpenAI mantendo prompt markdown, schema e metadados de auditoria. */
    private OpenAiRequest buildOpenAiRequest(StageContext<HypothesisMechanismInput> context) {
        Map<String, Object> data = context.input().promptData();
        String promptResource = properties.promptResource();
        BackendPromptSchemaTemplate template = BackendPromptSchemaTemplate.fromPromptData(
                data,
                properties.model(),
                properties.schemaName(),
                loadResource(promptResource),
                loadResource(properties.schemaResource()));
        String prompt = promptTemplateResolver.resolve(template.promptMarkdownContent(), data, promptResource);
        String requestBodyJson = buildResponsesApiRequest(prompt, template.schemaJson(), template.schemaName(), template.model());
        return new OpenAiRequest(
                template.model(),
                prompt,
                requestBodyJson,
                template.schemaName(),
                template.schemaJson(),
                template.promptMarkdownContent(),
                Map.of(
                        "stageCode", context.execution().stageCode(),
                        "idJob", context.execution().idJob(),
                        "marketNicheId", context.execution().aggregateId()),
                properties.serviceTier());
    }

    /** Serializa o corpo compatível com Responses API usando schema JSON estrito. */
    private String buildResponsesApiRequest(String prompt, String schemaJson, String schemaName, String model) {
        try {
            Object schema = objectMapper.readValue(schemaJson, Object.class);
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", schemaName);
            format.put("schema", schema);
            format.put("strict", true);
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("format", format);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", prompt);
            body.put("text", text);
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            log.error("Could not build OpenAI Responses API request for hypothesis mechanism. schemaName={}", schemaName, ex);
            throw new StageWorkerException("Could not build OpenAI Responses API request for hypothesis mechanism", ex);
        }
    }

    /** Converte valores de placeholder para texto ou JSON formatado antes da substituição. */
    private String toJsonOrText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize hypothesis mechanism prompt value; using toString fallback. valueType={}", value.getClass().getName(), ex);
            return value.toString();
        }
    }

    /** Lê recursos do classpath usados como prompt markdown ou schema da etapa. */
    private String loadResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Could not load resource for hypothesis mechanism. path={}", path, ex);
            throw new StageWorkerException("Could not load resource " + path, ex);
        }
    }
}
