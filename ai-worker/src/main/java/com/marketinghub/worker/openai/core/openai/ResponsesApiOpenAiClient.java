package com.marketinghub.worker.openai.core.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.OpenAiHttpException;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Responsabilidade: executar chamadas síncronas para a OpenAI Responses API no core OpenAI. */
public class ResponsesApiOpenAiClient implements OpenAiClientPort {

    private static final Logger log = LoggerFactory.getLogger(ResponsesApiOpenAiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OpenAiClientProperties properties;
    private final OpenAiCostEstimator costEstimator;
    private final Map<String, OpenAiResult<String>> resultCache = new ConcurrentHashMap<>();

    /** Inicializa o cliente com WebClient, ObjectMapper, propriedades e estimador de custo. */
    public ResponsesApiOpenAiClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            OpenAiClientProperties properties
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.costEstimator = new OpenAiCostEstimator(
                properties.inputUsdPerMillionTokens(),
                properties.outputUsdPerMillionTokens()
        );
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /** Envia o request cru para a OpenAI e devolve os dados de despacho para auditoria no backend. */
    @Override
    public OpenAiDispatch dispatch(OpenAiRequest request) {
        String requestBodyJson = request.requestBodyJson();
        String marketingHubJobId = marketingHubJobId(request);
        try {
            Map<String, Object> requestBody = objectMapper.readValue(requestBodyJson, Map.class);
            log.info("Enviando request cru para OpenAI Responses API [jobId={}, schemaName={}, requestBodyJson={}]", marketingHubJobId, request.schemaName(), requestBodyJson);

            Map<String, Object> raw = webClient.post()
                    .uri("/responses")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(properties.timeout());

            if (raw == null) {
                throw new StageWorkerException("OpenAI returned an empty response");
            }

            String rawJson = objectMapper.writeValueAsString(raw);
            log.info("Resposta crua recebida da OpenAI Responses API [jobId={}, openAiJobId={}, rawResponseJson={}]", marketingHubJobId, stringValue(raw.get("id")), rawJson);
            String openAiJobId = stringValue(raw.get("id"));
            String modelResponse = extractModelResponse(raw);
            Integer inputTokens = extractInteger(raw, "usage", "input_tokens");
            Integer outputTokens = extractInteger(raw, "usage", "output_tokens");

            OpenAiResult<String> result = new OpenAiResult<>(
                    openAiJobId,
                    rawJson,
                    modelResponse,
                    modelResponse,
                    inputTokens,
                    outputTokens,
                    costEstimator.estimate(inputTokens, outputTokens)
            );

            if (openAiJobId != null) {
                resultCache.put(openAiJobId, result);
            }

            return new OpenAiDispatch(
                    openAiJobId,
                    request.prompt(),
                    request.schemaJson(),
                    request.requestBodyJson(),
                    request.promptMarkdownContent(),
                    Instant.now()
            );
        } catch (WebClientResponseException error) {
            log.error(
                    "Falha HTTP na OpenAI Responses API [jobId={}, schemaName={}, status={}, responseBody={}, requestBodyJson={}]",
                    marketingHubJobId,
                    request.schemaName(),
                    error.getStatusCode().value(),
                    error.getResponseBodyAsString(),
                    requestBodyJson,
                    error);
            throw new OpenAiHttpException(error.getStatusCode().value(), error.getResponseBodyAsString(), error);
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Invalid OpenAI request JSON", error);
        }
    }

    /** Recupera o idJob do Marketing Hub a partir dos metadados do request para correlacionar logs operacionais. */
    private String marketingHubJobId(OpenAiRequest request) {
        Object value = request.metadata().get("idJob");
        return value != null ? value.toString() : "<unknown>";
    }

    /** Recupera a resposta bruta da OpenAI armazenada localmente após o despacho síncrono. */
    @Override
    public OpenAiResult<String> awaitResult(OpenAiDispatch dispatch) {
        if (dispatch.openAiJobId() == null) {
            throw new StageWorkerException("Cannot await OpenAI result without openAiJobId");
        }

        OpenAiResult<String> result = resultCache.remove(dispatch.openAiJobId());

        if (result == null) {
            throw new StageWorkerException("OpenAI result not found in local cache: " + dispatch.openAiJobId());
        }

        return result;
    }

    /** Extrai o texto principal da resposta retornada pela OpenAI. */
    private String extractModelResponse(Map<String, Object> raw) {
        Object outputText = raw.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return outputText.toString();
        }

        Object output = raw.get("output");
        if (output instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();

            for (Object item : list) {
                if (item instanceof Map<?, ?> itemMap) {
                    Object content = itemMap.get("content");
                    if (content instanceof List<?> contentList) {
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
            }

            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }

        throw new StageWorkerException("Could not extract model response text from OpenAI response");
    }

    /** Extrai um número inteiro de uma chave filha dentro de uma chave pai na resposta da OpenAI. */
    private Integer extractInteger(Map<String, Object> raw, String parentKey, String childKey) {
        Object parent = raw.get(parentKey);
        if (!(parent instanceof Map<?, ?> map)) {
            return null;
        }

        Object value = map.get(childKey);
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }

        return null;
    }

    /** Converte valores recebidos da OpenAI para texto quando presentes. */
    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
