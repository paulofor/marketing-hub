package com.marketinghub.worker.experimentpromise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.prompt.PromptTemplateResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsável por transformar o contexto salvo no backend em opções de promessa usando prompt e schema versionados. */
@Component
public class ExperimentPromiseOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPromiseOpenAiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PromptTemplateResolver promptTemplateResolver;
    private final String model;
    private final String promptResource;
    private final String schemaResource;
    private final String schemaName;

    /** Inicializa o cliente da OpenAI com recursos externos de prompt e schema da etapa. */
    public ExperimentPromiseOpenAiClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.experiment-promise-model:gpt-5.2}") String model,
                                         @Value("${experiment-promise.worker.prompt-resource:prompts/experiment/promise-contract-options.md}") String promptResource,
                                         @Value("${experiment-promise.worker.schema-resource:prompts/experiment/promise-contract-options-schema.json}") String schemaResource,
                                         @Value("${experiment-promise.worker.schema-name:promise_contract_options}") String schemaName) {
        WebClient.Builder clientBuilder = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.promptTemplateResolver = new PromptTemplateResolver(this::loadResource, this::toJsonOrText);
        this.model = model;
        this.promptResource = promptResource;
        this.schemaResource = schemaResource;
        this.schemaName = schemaName;
    }

    /** Gera exatamente três opções comerciais para a solicitação assumida. */
    public List<ExperimentPromiseOptionDto> generate(ExperimentPromiseOptionsResponse request) {
        Map<String, Object> payload = buildPayload(request);
        log.info("Enviando request de promessa para OpenAI; requestId={} payload={}", request.requestId(), toJsonOrText(payload));
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();
        log.info("Resposta bruta da OpenAI para promessa; requestId={} response={}", request.requestId(), toJsonOrText(response));
        String content = response != null ? response.firstText() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI não retornou conteúdo para opções de promessa");
        }
        return parseOptions(content);
    }

    /** Monta o payload da Responses API com prompt markdown e schema JSON do classpath. */
    private Map<String, Object> buildPayload(ExperimentPromiseOptionsResponse request) {
        String template = loadResource(promptResource);
        String prompt = promptTemplateResolver.resolve(template, Map.of("prompt", request.prompt()), promptResource);
        Object schema = readSchema();

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", format);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", prompt);
        payload.put("text", text);
        if (OpenAiRequestUtils.supportsTemperature(model)) {
            payload.put("temperature", 0.4);
        }
        OpenAiRequestUtils.maybeAddReasoning(payload, model);
        return payload;
    }

    /** Converte o JSON da OpenAI para a lista de opções esperada pelo backend. */
    private List<ExperimentPromiseOptionDto> parseOptions(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode optionsNode = root.path("options");
            List<ExperimentPromiseOptionDto> options = objectMapper.readerForListOf(ExperimentPromiseOptionDto.class)
                    .readValue(optionsNode);
            if (options.size() != 3) {
                throw new IllegalStateException("OpenAI retornou " + options.size() + " opções; esperado=3");
            }
            return options;
        } catch (Exception ex) {
            log.error("Falha ao interpretar JSON de opções de promessa; operation=experiment-promise-parse", ex);
            throw new IllegalStateException("Resposta da OpenAI inválida para opções de promessa", ex);
        }
    }

    /** Lê o schema JSON rígido usado para validar a saída da OpenAI. */
    private Object readSchema() {
        try {
            return objectMapper.readValue(loadResource(schemaResource), Object.class);
        } catch (JsonProcessingException ex) {
            throw new StageWorkerException("Falha ao ler schema de opções de promessa", ex);
        }
    }

    /** Converte valores de placeholder e logs para texto ou JSON formatado. */
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
            return value.toString();
        }
    }

    /** Lê prompt e schema do classpath, no mesmo padrão dos workers GeraLanding. */
    private String loadResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new StageWorkerException("Falha ao carregar recurso de promessa: " + path, ex);
        }
    }
}
