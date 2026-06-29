package com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Integra a etapa persona-candidate-generator com a OpenAI Responses API usando saída estruturada. */
@Component
public class OpenAiPersonaCandidateGenerationClient implements PersonaCandidateGenerationClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiPersonaCandidateGenerationClient.class);
    private static final String RESPONSES_PATH = "/responses";
    private static final String SCHEMA_NAME = "oprm_nichocnae_v3_persona_candidate_generator";
    private static final String CONTAINER_OPENAI_KEY_FILE = "/run/secrets/openai_api_key";
    private static final String HOST_OPENAI_KEY_FILE = "/root/infra/openai-token/openai_api_key";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PersonaCandidateOpenAiProperties properties;
    private final PersonaCandidatePromptBuilder promptBuilder;
    private final PersonaCandidateSchemaLoader schemaLoader;
    private final String backendBaseUrl;

    /** Inicializa o cliente OpenAI com prompt, schema e propriedades da etapa. */
    public OpenAiPersonaCandidateGenerationClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            PersonaCandidateOpenAiProperties properties,
            PersonaCandidatePromptBuilder promptBuilder,
            PersonaCandidateSchemaLoader schemaLoader,
            @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.schemaLoader = schemaLoader;
        this.backendBaseUrl = backendBaseUrl == null || backendBaseUrl.isBlank() ? "http://191.252.181.168" : backendBaseUrl;
    }

    /** Gera personas candidatas por OpenAI e retorna o payload funcional validado pelo schema. */
    @Override
    public Map<String, Object> generate(PersonaCandidateGenerationRequest request) {
        String apiKey = resolveApiKey(request);
        if (apiKey.isBlank()) {
            log.error(
                    "OpenAI API key ausente para persona-candidate-generator (jobId={}, stageExecutionId={}, cnaeCode={}, baseUrl={}, model={}, serviceTier={}, apiKeyFileConfigurado={})",
                    request.jobId(),
                    request.stageExecutionId(),
                    request.cnaeCode(),
                    properties.baseUrl(),
                    properties.model(),
                    properties.serviceTier(),
                    !properties.apiKeyFile().isBlank());
            throw new IllegalStateException("OpenAI API key não configurada para persona-candidate-generator NichoCNAE v3.");
        }
        String url = properties.baseUrl() + RESPONSES_PATH;
        Map<String, Object> requestBody = buildRequestBody(promptBuilder.build(request), properties.model());
        String rawRequestBody = toJsonForLog(requestBody);
        logOpenAiRequest(url, request, rawRequestBody);
        sendBackendRequestAudit(request, rawRequestBody, requestBody);
        try {
            String rawResponseBody = responseBody(url, rawRequestBody, apiKey);
            logOpenAiResponse(url, request, rawResponseBody);
            sendBackendResponseAudit(request, rawResponseBody, null);
            Map<String, Object> raw = parseOpenAiResponse(rawResponseBody);
            if (raw == null) {
                log.error(
                        "OpenAI retornou corpo vazio para persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, model={}, serviceTier={})",
                        url,
                        request.jobId(),
                        request.stageExecutionId(),
                        request.cnaeCode(),
                        properties.model(),
                        properties.serviceTier());
                throw new IllegalStateException("OpenAI retornou corpo vazio para persona-candidate-generator NichoCNAE v3.");
            }
            String modelResponse = extractModelResponse(raw, request, url);
            return objectMapper.readValue(modelResponse, MAP_TYPE);
        } catch (RestClientResponseException ex) {
            logOpenAiHttpError(url, request, ex);
            sendBackendResponseAudit(request, ex.getResponseBodyAsString(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw new IllegalStateException("Falha na OpenAI ao gerar personas candidatas do NichoCNAE v3.", ex);
        } catch (RestClientException | JsonProcessingException | IllegalStateException | IllegalArgumentException ex) {
            log.error(
                    "Erro ao gerar personas candidatas com OpenAI (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, model={}, serviceTier={}, exceptionType={}, message={})",
                    url,
                    request.jobId(),
                    request.stageExecutionId(),
                    request.cnaeCode(),
                    properties.model(),
                    properties.serviceTier(),
                    ex.getClass().getName(),
                    ex.getMessage(),
                    ex);
            throw new IllegalStateException("Falha na OpenAI ao gerar personas candidatas do NichoCNAE v3.", ex);
        }
    }

    /** Resolve a chave por propriedade direta, segredo montado no container ou arquivo seguro do host. */
    String resolveApiKey(PersonaCandidateGenerationRequest request) {
        if (!properties.apiKey().isBlank()) {
            return properties.apiKey().trim();
        }
        for (Path apiKeyPath : apiKeyCandidatePaths()) {
            String apiKey = readApiKeyFile(apiKeyPath, request);
            if (!apiKey.isBlank()) {
                log.info(
                        "OpenAI API key resolvida por arquivo seguro para persona-candidate-generator (apiKeyFile={}, jobId={}, stageExecutionId={}, cnaeCode={})",
                        apiKeyPath,
                        request.jobId(),
                        request.stageExecutionId(),
                        request.cnaeCode());
                return apiKey;
            }
        }
        log.warn(
                "Nenhuma origem de OpenAI API key disponível para persona-candidate-generator (jobId={}, stageExecutionId={}, cnaeCode={}, candidateFiles={})",
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                apiKeyCandidatePaths());
        return "";
    }

    /** Lista os caminhos seguros aceitos para a chave OpenAI sem duplicar a mesma origem. */
    private List<Path> apiKeyCandidatePaths() {
        List<String> candidates = List.of(properties.apiKeyFile(), CONTAINER_OPENAI_KEY_FILE, HOST_OPENAI_KEY_FILE);
        return candidates.stream()
                .filter(candidate -> !candidate.isBlank())
                .map(Path::of)
                .distinct()
                .toList();
    }

    /** Lê um arquivo de chave quando ele existe e registra falhas sem expor o segredo. */
    private String readApiKeyFile(Path apiKeyPath, PersonaCandidateGenerationRequest request) {
        if (!Files.isRegularFile(apiKeyPath) || !Files.isReadable(apiKeyPath)) {
            log.debug(
                    "Arquivo de chave OpenAI indisponível para persona-candidate-generator (apiKeyFile={}, jobId={}, stageExecutionId={}, cnaeCode={})",
                    apiKeyPath,
                    request.jobId(),
                    request.stageExecutionId(),
                    request.cnaeCode());
            return "";
        }
        try {
            return Files.readString(apiKeyPath).trim();
        } catch (IOException ex) {
            log.error(
                    "Erro ao ler arquivo de chave OpenAI da etapa persona-candidate-generator (apiKeyFile={}, jobId={}, stageExecutionId={}, cnaeCode={})",
                    apiKeyPath,
                    request.jobId(),
                    request.stageExecutionId(),
                    request.cnaeCode(),
                    ex);
            return "";
        }
    }

    /** Monta o corpo da Responses API com JSON Schema estrito, Flex Processing e pesquisa web opcional. */
    Map<String, Object> buildRequestBody(String prompt, String model) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", SCHEMA_NAME);
        format.put("schema", schemaLoader.load());
        format.put("strict", true);

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", format);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", prompt);
        body.put("text", text);
        body.put("service_tier", properties.serviceTier());
        if (properties.webSearchEnabled()) {
            body.put("tools", List.of(Map.of("type", "web_search")));
            body.put("include", List.of("web_search_call.results"));
        }
        return body;
    }

    /** Registra o payload enviado à OpenAI sem expor credenciais de autenticação. */
    private void logOpenAiRequest(String url, PersonaCandidateGenerationRequest request, String rawRequestBody) {
        log.info(
                "Request OpenAI persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, payload={})",
                url,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                rawRequestBody);
    }

    /** Registra a resposta bruta recebida da OpenAI para auditoria operacional. */
    private void logOpenAiResponse(String url, PersonaCandidateGenerationRequest request, String rawResponseBody) {
        log.info(
                "Response OpenAI persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, payload={})",
                url,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                rawResponseBody);
    }

    /** Envia ao backend exatamente o request bruto encaminhado à OpenAI pelo endpoint recebeRequest. */
    private void sendBackendRequestAudit(PersonaCandidateGenerationRequest request, String rawRequestBody, Map<String, Object> requestBody) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request", rawRequestBody);
        payload.put("plataforma", "OPENAI_RESPONSES_API");
        payload.put("prompt", String.valueOf(requestBody.getOrDefault("input", "")));
        payload.put("schema", toJsonForLog(schemaLoader.load()));
        String endpoint = backendStageExecutionUrl(request, "recebeRequest");
        log.info(
                "Enviando request OpenAI persona-candidate-generator ao backend (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={})",
                endpoint,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode());
        restClient.post().uri(URI.create(endpoint)).body(payload).retrieve().toBodilessEntity();
    }

    /** Envia ao backend exatamente a resposta bruta da OpenAI ou o erro capturado pelo endpoint recebeResponse. */
    private void sendBackendResponseAudit(PersonaCandidateGenerationRequest request, String rawResponseBody, String errorMessage) {
        Map<String, Object> responseBody = parseOpenAiResponse(rawResponseBody);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("response", rawResponseBody);
        payload.put("descricaoErro", errorMessage);
        payload.put("quantidadeTokenEntrada", tokenUsage(responseBody, "input_tokens"));
        payload.put("quantidadeTokenSaida", tokenUsage(responseBody, "output_tokens"));
        payload.put("custo", null);
        payload.put("modelo", properties.model());
        String endpoint = backendStageExecutionUrl(request, "recebeResponse");
        log.info(
                "Enviando response OpenAI persona-candidate-generator ao backend (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, hasError={})",
                endpoint,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                errorMessage != null && !errorMessage.isBlank());
        restClient.post().uri(URI.create(endpoint)).body(payload).retrieve().toBodilessEntity();
    }

    /** Monta a URL canônica do callback backend para auditoria request/response. */
    private String backendStageExecutionUrl(PersonaCandidateGenerationRequest request, String callback) {
        String base = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1) : backendBaseUrl;
        return base + "/api/internal/oprmcoletormei/nichocnae/v3/persona-candidate-generator/stage-executions/"
                + request.cnaeCode() + "/" + request.jobId() + "/" + callback;
    }

    /** Extrai tokens retornados pela OpenAI quando o campo usage estiver disponível. */
    private Long tokenUsage(Map<String, Object> responseBody, String field) {
        if (responseBody != null && responseBody.get("usage") instanceof Map<?, ?> usage) {
            Object value = usage.get(field);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    /** Serializa payloads para log preservando diagnóstico mesmo quando houver valor não serializável. */
    private String toJsonForLog(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao serializar payload OpenAI para log.", ex);
            return String.valueOf(payload);
        }
    }

    /** Executa a requisição HTTP para a Responses API. */
    private String responseBody(String url, String rawRequestBody, String apiKey) {
        return restClient.post()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(rawRequestBody)
                .retrieve()
                .body(String.class);
    }

    /** Converte a resposta bruta da OpenAI para mapa apenas depois da auditoria crua. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAiResponse(String rawResponseBody) {
        if (rawResponseBody == null || rawResponseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawResponseBody, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao interpretar resposta bruta OpenAI para métricas/extração.", ex);
            return null;
        }
    }

    /** Registra detalhes HTTP quando a OpenAI responde com erro. */
    private void logOpenAiHttpError(String url, PersonaCandidateGenerationRequest request, RestClientResponseException ex) {
        log.error(
                "Erro HTTP da OpenAI em persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, model={}, serviceTier={}, statusCode={}, statusText={}, responseHeaders={}, responseBody={})",
                url,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                properties.model(),
                properties.serviceTier(),
                ex.getStatusCode().value(),
                ex.getStatusText(),
                ex.getResponseHeaders(),
                ex.getResponseBodyAsString(),
                ex);
    }

    /** Extrai texto JSON da resposta da OpenAI aceitando output_text ou output estruturado. */
    private String extractModelResponse(Map<String, Object> raw, PersonaCandidateGenerationRequest request, String url) {
        Object outputText = raw.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            log.info(
                    "JSON extraído de output_text da OpenAI para persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, outputLength={})",
                    url,
                    request.jobId(),
                    request.stageExecutionId(),
                    request.cnaeCode(),
                    outputText.toString().length());
            return outputText.toString();
        }
        Object output = raw.get("output");
        if (output instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            for (Object item : list) {
                appendOutputItemText(builder, item);
            }
            if (!builder.isEmpty()) {
                log.info(
                        "JSON extraído de output estruturado da OpenAI para persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, outputItems={}, outputLength={})",
                        url,
                        request.jobId(),
                        request.stageExecutionId(),
                        request.cnaeCode(),
                        list.size(),
                        builder.length());
                return builder.toString();
            }
        }
        log.error(
                "Resposta OpenAI sem texto JSON extraível para persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, responseKeys={}, payload={})",
                url,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                raw.keySet(),
                toJsonForLog(raw));
        throw new IllegalStateException("Não foi possível extrair JSON da resposta OpenAI de personas candidatas.");
    }

    /** Acrescenta conteúdo textual de um item de output no buffer final. */
    private void appendOutputItemText(StringBuilder builder, Object item) {
        if (!(item instanceof Map<?, ?> itemMap)) {
            return;
        }
        Object content = itemMap.get("content");
        if (!(content instanceof List<?> contentList)) {
            return;
        }
        for (Object contentItem : contentList) {
            if (contentItem instanceof Map<?, ?> contentMap && contentMap.get("text") != null) {
                builder.append(contentMap.get("text"));
            }
        }
    }
}
