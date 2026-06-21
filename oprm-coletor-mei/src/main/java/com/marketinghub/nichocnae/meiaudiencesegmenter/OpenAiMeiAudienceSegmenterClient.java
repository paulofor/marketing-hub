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
import org.springframework.web.client.RestClientResponseException;

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
            log.error(
                    "Configuração operacional ausente antes de chamar OpenAI (module={}, operation={}, expectedVariable={}, fallbackVariable={}, researchCycleId={}, cnaeCode={})",
                    MeiAudienceSegmenterOperationalGuard.MODULE,
                    MeiAudienceSegmenterOperationalGuard.OPERATION,
                    properties.expectedApiKeyVariable(),
                    properties.fallbackApiKeyVariable(),
                    input.researchCycleId(),
                    input.cnaeCode());
            throw new MeiAudienceSegmenterOperationalException(
                    "Falha operacional na etapa mei-audience-segmenter: variável "
                            + properties.expectedApiKeyVariable()
                            + " ausente e fallback "
                            + properties.fallbackApiKeyVariable()
                            + " indisponível. module="
                            + MeiAudienceSegmenterOperationalGuard.MODULE
                            + ", operation="
                            + MeiAudienceSegmenterOperationalGuard.OPERATION
                            + ", expectedVariable="
                            + properties.expectedApiKeyVariable()
                            + ", fallbackVariable="
                            + properties.fallbackApiKeyVariable()
                            + ", researchCycleId="
                            + input.researchCycleId()
                            + ".");
        }
        String prompt = promptBuilder.buildPrompt(input);
        String model = resolveModel(input);
        Map<String, Object> requestBody = buildRequestBody(prompt, model);
        String url = properties.baseUrl() + RESPONSES_PATH;
        try {
            return generateValidatedDraft(input, url, requestBody, apiKey, false);
        } catch (MeiAudienceSegmenterOperationalException ex) {
            throw ex;
        } catch (RestClientException | JsonProcessingException | IllegalStateException | IllegalArgumentException ex) {
            log.error(
                    "Erro ao segmentar público MEI/autônomo com OpenAI (endpoint={}, researchCycleId={}, cnaeCode={})",
                    url,
                    input.researchCycleId(),
                    input.cnaeCode(),
                    ex);
            String operationalMessage = buildOperationalFailureMessage(input, model, url, ex);
            throw new IllegalStateException(operationalMessage, ex);
        }
    }

    /** Gera, pré-valida e regenera uma vez quando a saída vier contaminada por termo proibido. */
    private MeiAudienceSegmentDraft generateValidatedDraft(
            MeiAudienceSegmenterPending input, String url, Map<String, Object> requestBody, String apiKey, boolean correctiveAttempt)
            throws JsonProcessingException {
        Map<String, Object> raw = responseBody(url, requestBody, apiKey);
        if (raw == null) {
            throw new IllegalStateException("OpenAI retornou corpo vazio para segmentação MEI/autônomo.");
        }
        String rawModelResponse = extractModelResponse(raw);
        MeiAudienceSegmentDraft draft = objectMapper.readValue(rawModelResponse, MeiAudienceSegmentDraft.class);
        String forbiddenTerm = validator.firstForbiddenTerm(draft);
        if (forbiddenTerm != null && !correctiveAttempt) {
            log.warn(
                    "Pré-validação bloqueou segmentação MEI/autônomo contaminada; regenerando uma vez (researchCycleId={}, cnaeCode={}, forbiddenTerm={})",
                    input.researchCycleId(),
                    input.cnaeCode(),
                    forbiddenTerm);
            String correctivePrompt = promptBuilder.buildCorrectivePrompt(input, forbiddenTerm);
            Map<String, Object> correctiveRequestBody = buildRequestBody(correctivePrompt, resolveModel(input));
            return generateValidatedDraft(input, url, correctiveRequestBody, apiKey, true);
        }
        validator.validate(input, draft);
        return draft;
    }

    /** Monta mensagem curta com causa-raiz suficiente para ação operacional no backend. */
    String buildOperationalFailureMessage(
            MeiAudienceSegmenterPending input, String model, String endpoint, Exception ex) {
        StringBuilder message = new StringBuilder("Falha ao segmentar público MEI/autônomo com OpenAI");
        message.append("; tipo=").append(ex.getClass().getSimpleName());
        message.append("; causaRaiz=").append(summarize(rootCauseMessage(ex)));
        message.append("; researchCycleId=").append(input.researchCycleId());
        message.append("; routineCardId=").append(input.routineCardId());
        message.append("; modeloOpenAI=").append(model);
        message.append("; endpoint=").append(endpoint);
        if (ex instanceof RestClientResponseException responseException) {
            message.append("; httpStatus=").append(responseException.getStatusCode().value());
            message.append("; httpBody=").append(summarize(responseException.getResponseBodyAsString()));
        }
        return message.toString();
    }

    /** Encontra a mensagem da causa-raiz preservando o tipo quando não houver mensagem textual. */
    private String rootCauseMessage(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root.getMessage() == null || root.getMessage().isBlank()) {
            return root.getClass().getSimpleName();
        }
        return root.getMessage();
    }

    /** Resume textos de erro para evitar payload excessivo no registro operacional. */
    private String summarize(String value) {
        if (value == null || value.isBlank()) {
            return "indisponível";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "...";
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

    /** Resolve o modelo efetivo priorizando a configuração operacional recebida do backend. */
    String resolveModel(MeiAudienceSegmenterPending input) {
        if (input != null && input.openAiModelCode() != null && !input.openAiModelCode().isBlank()) {
            return input.openAiModelCode().trim();
        }
        return properties.model();
    }

    /** Monta corpo da Responses API com schema JSON estrito. */
    private Map<String, Object> buildRequestBody(String prompt, String model) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", SCHEMA_NAME);
        format.put("schema", schema.buildSchema());
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
