package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Qualifica fontes candidatas do source-searcher com OpenAI sem transferir a orquestração da busca. */
@Component
public class OpenAiSourceEvidenceQualifier implements SourceEvidenceQualifier {
    private static final Logger log = LoggerFactory.getLogger(OpenAiSourceEvidenceQualifier.class);
    private static final String RESPONSES_PATH = "/responses";
    private static final String SCHEMA_NAME = "oprm_nichocnae_v3_source_searcher";
    private static final String CONTAINER_OPENAI_KEY_FILE = "/run/secrets/openai_api_key";
    private static final String HOST_OPENAI_KEY_FILE = "/root/infra/openai-token/openai_api_key";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SourceSearcherOpenAiProperties properties;
    private final SourceSearcherPromptBuilder promptBuilder;
    private final SourceSearcherSchemaLoader schemaLoader;
    private final String backendBaseUrl;

    /** Inicializa a qualificação por OpenAI com prompt/schema versionados e callbacks de auditoria. */
    public OpenAiSourceEvidenceQualifier(
            RestClient restClient,
            ObjectMapper objectMapper,
            SourceSearcherOpenAiProperties properties,
            SourceSearcherPromptBuilder promptBuilder,
            SourceSearcherSchemaLoader schemaLoader,
            @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.schemaLoader = schemaLoader;
        this.backendBaseUrl = backendBaseUrl == null || backendBaseUrl.isBlank() ? "http://191.252.181.168" : backendBaseUrl;
    }

    /** Seleciona fontes candidatas com apoio da OpenAI e retorna fallback determinístico quando a IA não puder operar. */
    @Override
    public List<Map<String, Object>> qualify(
            StageContext context,
            List<Map<String, Object>> plannedQueries,
            List<Map<String, Object>> searchAttempts,
            List<Map<String, Object>> deterministicSelectedSources) {
        if (!properties.enabled()) {
            return deterministicSelectedSources;
        }
        String apiKey = resolveApiKey(context);
        if (apiKey.isBlank()) {
            log.warn("OpenAI source-searcher desabilitada por ausência de chave (jobId={}, stageExecutionId={}, cnaeCode={})",
                    context.jobId(), context.stageExecutionId(), context.input().getOrDefault("cnaeCode", ""));
            return deterministicSelectedSources;
        }
        String url = properties.baseUrl() + RESPONSES_PATH;
        Map<String, Object> requestBody = buildRequestBody(promptBuilder.build(context, plannedQueries, searchAttempts));
        String rawRequestBody = toJsonForLog(requestBody);
        log.info("Request OpenAI source-searcher (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, payload={})",
                url, context.jobId(), context.stageExecutionId(), context.input().getOrDefault("cnaeCode", ""), rawRequestBody);
        sendBackendRequestAudit(context, rawRequestBody, requestBody);
        try {
            String rawResponseBody = responseBody(url, rawRequestBody, apiKey);
            log.info("Response OpenAI source-searcher (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, payload={})",
                    url, context.jobId(), context.stageExecutionId(), context.input().getOrDefault("cnaeCode", ""), rawResponseBody);
            sendBackendResponseAudit(context, rawResponseBody, null);
            return selectedSourcesFromOpenAi(rawResponseBody, deterministicSelectedSources);
        } catch (RestClientResponseException ex) {
            log.error(
                    "Erro HTTP da OpenAI em source-searcher (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, model={}, serviceTier={}, statusCode={}, responseBody={})",
                    url,
                    context.jobId(),
                    context.stageExecutionId(),
                    context.input().getOrDefault("cnaeCode", ""),
                    properties.model(),
                    properties.serviceTier(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex);
            sendBackendResponseAudit(context, ex.getResponseBodyAsString(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return deterministicSelectedSources;
        } catch (RestClientException | IllegalStateException | IllegalArgumentException ex) {
            log.error("Falha ao qualificar fontes com OpenAI source-searcher (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={})",
                    url, context.jobId(), context.stageExecutionId(), context.input().getOrDefault("cnaeCode", ""), ex);
            sendBackendResponseAudit(context, "", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return deterministicSelectedSources;
        }
    }

    /** Resolve a chave por propriedade direta, segredo montado no container ou arquivo seguro do host. */
    String resolveApiKey(StageContext context) {
        if (!properties.apiKey().isBlank()) {
            return properties.apiKey().trim();
        }
        for (Path apiKeyPath : apiKeyCandidatePaths()) {
            String apiKey = readApiKeyFile(apiKeyPath, context);
            if (!apiKey.isBlank()) {
                return apiKey;
            }
        }
        return "";
    }

    /** Lista caminhos aceitos para a chave OpenAI sem duplicar origens. */
    private List<Path> apiKeyCandidatePaths() {
        return List.of(properties.apiKeyFile(), CONTAINER_OPENAI_KEY_FILE, HOST_OPENAI_KEY_FILE).stream()
                .filter(candidate -> !candidate.isBlank())
                .map(Path::of)
                .distinct()
                .toList();
    }

    /** Lê arquivo de chave sem expor segredo em log. */
    private String readApiKeyFile(Path apiKeyPath, StageContext context) {
        if (!Files.isRegularFile(apiKeyPath) || !Files.isReadable(apiKeyPath)) {
            return "";
        }
        try {
            return Files.readString(apiKeyPath).trim();
        } catch (IOException ex) {
            log.error("Erro ao ler arquivo de chave OpenAI da etapa source-searcher (apiKeyFile={}, jobId={}, stageExecutionId={})",
                    apiKeyPath, context.jobId(), context.stageExecutionId(), ex);
            return "";
        }
    }

    /** Monta o corpo da Responses API com schema estrito e Flex Processing. */
    Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", SCHEMA_NAME);
        format.put("schema", schemaLoader.load());
        format.put("strict", true);
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", format);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("input", prompt);
        body.put("text", text);
        body.put("service_tier", properties.serviceTier());
        return body;
    }

    /** Executa a chamada HTTP para a Responses API. */
    private String responseBody(String url, String rawRequestBody, String apiKey) {
        return restClient.post()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(rawRequestBody)
                .retrieve()
                .body(String.class);
    }

    /** Extrai e valida fontes aprovadas pela resposta da OpenAI. */
    private List<Map<String, Object>> selectedSourcesFromOpenAi(String rawResponseBody, List<Map<String, Object>> deterministicSelectedSources) {
        Map<String, Object> raw = parseOpenAiResponse(rawResponseBody);
        if (raw == null) {
            return deterministicSelectedSources;
        }
        String modelResponse = extractModelResponse(raw);
        try {
            Map<String, Object> payload = objectMapper.readValue(modelResponse, MAP_TYPE);
            if (!Boolean.TRUE.equals(payload.get("enoughEvidence"))) {
                return deterministicSelectedSources;
            }
            Object selected = payload.get("selectedSources");
            if (!(selected instanceof List<?> selectedList)) {
                return deterministicSelectedSources;
            }
            List<Map<String, Object>> sources = new ArrayList<>();
            for (Object item : selectedList) {
                if (item instanceof Map<?, ?> itemMap && Boolean.TRUE.equals(itemMap.get("approved"))) {
                    Map<String, Object> source = new LinkedHashMap<>();
                    itemMap.forEach((key, value) -> source.put(String.valueOf(key), value));
                    source.put("aiQualified", true);
                    sources.add(source);
                }
            }
            return sources.isEmpty() ? deterministicSelectedSources : sources;
        } catch (JsonProcessingException ex) {
            log.error("Falha ao interpretar JSON funcional da OpenAI source-searcher.", ex);
            return deterministicSelectedSources;
        }
    }

    /** Converte a resposta bruta da OpenAI para mapa. */
    private Map<String, Object> parseOpenAiResponse(String rawResponseBody) {
        if (rawResponseBody == null || rawResponseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawResponseBody, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao interpretar resposta bruta OpenAI source-searcher.", ex);
            return null;
        }
    }

    /** Extrai texto JSON da resposta da OpenAI aceitando output_text ou output estruturado. */
    private String extractModelResponse(Map<String, Object> raw) {
        Object outputText = raw.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return outputText.toString();
        }
        Object output = raw.get("output");
        if (output instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            for (Object item : list) {
                appendOutputItemText(builder, item);
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        throw new IllegalStateException("Não foi possível extrair JSON da resposta OpenAI do source-searcher.");
    }

    /** Acrescenta conteúdo textual de um item de output no buffer final. */
    private void appendOutputItemText(StringBuilder builder, Object item) {
        if (!(item instanceof Map<?, ?> itemMap) || !(itemMap.get("content") instanceof List<?> contentList)) {
            return;
        }
        for (Object contentItem : contentList) {
            if (contentItem instanceof Map<?, ?> contentMap && contentMap.get("text") != null) {
                builder.append(contentMap.get("text"));
            }
        }
    }

    /** Envia ao backend exatamente o request bruto encaminhado à OpenAI. */
    private void sendBackendRequestAudit(StageContext context, String rawRequestBody, Map<String, Object> requestBody) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("request", rawRequestBody);
            payload.put("plataforma", "OPENAI_RESPONSES_API");
            payload.put("prompt", String.valueOf(requestBody.getOrDefault("input", "")));
            payload.put("schema", toJsonForLog(schemaLoader.load()));
            restClient.post().uri(URI.create(backendStageExecutionUrl(context, "recebeRequest"))).body(payload).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Falha ao auditar request OpenAI source-searcher no backend (jobId={}, stageExecutionId={})",
                    context.jobId(), context.stageExecutionId(), ex);
        }
    }

    /** Envia ao backend a resposta bruta da OpenAI ou erro capturado. */
    private void sendBackendResponseAudit(StageContext context, String rawResponseBody, String errorMessage) {
        try {
            Map<String, Object> responseBody = parseOpenAiResponse(rawResponseBody);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("response", rawResponseBody);
            payload.put("descricaoErro", errorMessage);
            payload.put("quantidadeTokenEntrada", tokenUsage(responseBody, "input_tokens"));
            payload.put("quantidadeTokenSaida", tokenUsage(responseBody, "output_tokens"));
            payload.put("custo", null);
            payload.put("modelo", properties.model());
            restClient.post().uri(URI.create(backendStageExecutionUrl(context, "recebeResponse"))).body(payload).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Falha ao auditar response OpenAI source-searcher no backend (jobId={}, stageExecutionId={}, hasError={})",
                    context.jobId(), context.stageExecutionId(), errorMessage != null && !errorMessage.isBlank(), ex);
        }
    }

    /** Monta a URL canônica do callback backend para auditoria request/response. */
    private String backendStageExecutionUrl(StageContext context, String callback) {
        String base = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1) : backendBaseUrl;
        return base + "/api/internal/oprmcoletormei/nichocnae/v3/source-searcher/stage-executions/"
                + context.input().getOrDefault("cnaeCode", "") + "/" + context.jobId() + "/" + callback;
    }

    /** Extrai tokens retornados pela OpenAI quando disponíveis. */
    private Long tokenUsage(Map<String, Object> responseBody, String field) {
        if (responseBody != null && responseBody.get("usage") instanceof Map<?, ?> usage && usage.get(field) instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /** Serializa payloads para log preservando diagnóstico quando houver valor não serializável. */
    private String toJsonForLog(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao serializar payload OpenAI source-searcher para log.", ex);
            return String.valueOf(payload);
        }
    }
}
