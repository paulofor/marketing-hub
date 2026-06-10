package com.marketinghub.nichocnae.meiaudiencesegmenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Executa chamada à OpenAI Responses API para segmentar comportamento MEI/autônomo com JSON estruturado. */
@Component
public class OpenAiMeiAudienceSegmenterClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiMeiAudienceSegmenterClient.class);
    private static final String RESPONSES_PATH = "/responses";
    private static final String SCHEMA_NAME = "oprm_mei_audience_segmenter";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MeiAudienceSegmenterOpenAiProperties properties;
    private final MeiAudienceSegmenterPromptBuilder promptBuilder;
    private final MeiAudienceSegmenterSchema schema;
    private final MeiAudienceSegmenterValidator validator;

    /** Inicializa o cliente OpenAI com prompt, schema e validator da segmentação MEI/autônomo. */
    public OpenAiMeiAudienceSegmenterClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            MeiAudienceSegmenterOpenAiProperties properties,
            MeiAudienceSegmenterPromptBuilder promptBuilder,
            MeiAudienceSegmenterSchema schema,
            MeiAudienceSegmenterValidator validator) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.schema = schema;
        this.validator = validator;
    }

    /** Gera segmentação por modelo de texto, extrai JSON estruturado e valida antes de persistir. */
    public MeiAudienceSegmentDraft segment(MeiAudienceSegmenterPending input) {
        String apiKey = resolveApiKey(input);
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPRM mei-audience-segmenter OpenAI API key não configurada.");
        }
        String prompt = promptBuilder.buildPrompt(input);
        Map<String, Object> requestBody = buildRequestBody(prompt);
        String url = properties.baseUrl() + RESPONSES_PATH;
        try {
            Map<String, Object> raw = responseBody(url, requestBody, apiKey);
            if (raw == null) {
                throw new IllegalStateException("OpenAI retornou corpo vazio para segmentação MEI/autônomo.");
            }
            String rawModelResponse = extractModelResponse(raw);
            MeiAudienceSegmentDraft draft = objectMapper.readValue(rawModelResponse, MeiAudienceSegmentDraft.class);
            validator.validate(input, draft);
            return draft;
        } catch (RestClientException | JsonProcessingException | IllegalStateException | IllegalArgumentException ex) {
            log.error(
                    "Erro ao segmentar público MEI/autônomo com OpenAI (endpoint={}, researchCycleId={}, cnaeCode={})",
                    url,
                    input.researchCycleId(),
                    input.cnaeCode(),
                    ex);
            throw new IllegalStateException("Falha ao segmentar público MEI/autônomo com OpenAI.", ex);
        }
    }

    /** Resolve chave OpenAI por variável direta ou arquivo montado no host. */
    String resolveApiKey(MeiAudienceSegmenterPending input) {
        if (!properties.apiKey().isBlank()) {
            return properties.apiKey().trim();
        }
        if (properties.apiKeyFile().isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(properties.apiKeyFile())).trim();
        } catch (IOException ex) {
            log.error(
                    "Erro ao ler arquivo de chave OpenAI da segmentação MEI/autônomo (apiKeyFile={}, researchCycleId={}, cnaeCode={})",
                    properties.apiKeyFile(),
                    input.researchCycleId(),
                    input.cnaeCode(),
                    ex);
            throw new IllegalStateException("Falha ao ler arquivo de chave OpenAI da segmentação MEI/autônomo.", ex);
        }
    }

    /** Executa a requisição HTTP e isola o cast seguro do corpo retornado pelo RestClient. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> responseBody(String url, Map<String, Object> requestBody, String apiKey) {
        Map<?, ?> response = restClient.post()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);
        return response == null ? null : (Map<String, Object>) response;
    }

    /** Monta corpo da Responses API com schema JSON estrito. */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", SCHEMA_NAME);
        format.put("schema", schema.buildSchema());
        format.put("strict", true);

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", format);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("input", prompt);
        body.put("text", text);
        return body;
    }

    /** Extrai o texto do modelo cobrindo os formatos comuns da Responses API. */
    private String extractModelResponse(Map<String, Object> raw) {
        Object outputText = raw.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return outputText.toString();
        }
        Object output = raw.get("output");
        if (output instanceof List<?> outputList) {
            StringBuilder builder = new StringBuilder();
            for (Object item : outputList) {
                appendOutputItemText(builder, item);
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        throw new IllegalStateException("Resposta OpenAI sem output_text para segmentação MEI/autônomo.");
    }

    /** Extrai textos de itens aninhados da Responses API. */
    private void appendOutputItemText(StringBuilder builder, Object item) {
        if (!(item instanceof Map<?, ?> itemMap)) {
            return;
        }
        Object content = itemMap.get("content");
        if (!(content instanceof List<?> contentList)) {
            return;
        }
        for (Object contentItem : contentList) {
            if (contentItem instanceof Map<?, ?> contentMap) {
                Object text = contentMap.get("text");
                if (text != null) {
                    builder.append(text);
                }
            }
        }
    }
}
