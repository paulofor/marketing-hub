package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    /** Inicializa o cliente OpenAI com prompt, schema e propriedades da etapa. */
    public OpenAiPersonaCandidateGenerationClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            PersonaCandidateOpenAiProperties properties,
            PersonaCandidatePromptBuilder promptBuilder,
            PersonaCandidateSchemaLoader schemaLoader) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.schemaLoader = schemaLoader;
    }

    /** Gera personas candidatas por OpenAI e retorna o payload funcional validado pelo schema. */
    @Override
    public Map<String, Object> generate(PersonaCandidateGenerationRequest request) {
        String apiKey = resolveApiKey(request);
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key não configurada para persona-candidate-generator NichoCNAE v3.");
        }
        String url = properties.baseUrl() + RESPONSES_PATH;
        Map<String, Object> requestBody = buildRequestBody(promptBuilder.build(request), properties.model());
        logOpenAiRequest(url, request, requestBody);
        try {
            Map<String, Object> raw = responseBody(url, requestBody, apiKey);
            logOpenAiResponse(url, request, raw);
            if (raw == null) {
                throw new IllegalStateException("OpenAI retornou corpo vazio para persona-candidate-generator NichoCNAE v3.");
            }
            return objectMapper.readValue(extractModelResponse(raw), MAP_TYPE);
        } catch (RestClientException | JsonProcessingException | IllegalStateException | IllegalArgumentException ex) {
            log.error(
                    "Erro ao gerar personas candidatas com OpenAI (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, model={}, serviceTier={})",
                    url,
                    request.jobId(),
                    request.stageExecutionId(),
                    request.cnaeCode(),
                    properties.model(),
                    properties.serviceTier(),
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
                return apiKey;
            }
        }
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

    /** Monta o corpo da Responses API com JSON Schema estrito e Flex Processing. */
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
        return body;
    }

    /** Registra o payload enviado à OpenAI sem expor credenciais de autenticação. */
    private void logOpenAiRequest(String url, PersonaCandidateGenerationRequest request, Map<String, Object> requestBody) {
        log.info(
                "Request OpenAI persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, payload={})",
                url,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                toJsonForLog(requestBody));
    }

    /** Registra a resposta bruta recebida da OpenAI para auditoria operacional. */
    private void logOpenAiResponse(String url, PersonaCandidateGenerationRequest request, Map<String, Object> raw) {
        log.info(
                "Response OpenAI persona-candidate-generator (endpoint={}, jobId={}, stageExecutionId={}, cnaeCode={}, payload={})",
                url,
                request.jobId(),
                request.stageExecutionId(),
                request.cnaeCode(),
                toJsonForLog(raw));
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
