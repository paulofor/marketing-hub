package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.OpenAiHttpException;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Responsabilidade: executar chamadas síncronas na OpenAI Images API para a etapa imagegeneration. */
public class ImageGenerationOpenAiClient implements OpenAiClientPort {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationOpenAiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ImageGenerationWorkerProperties properties;
    private final boolean enabled;
    private final Map<String, PendingImageDispatch> pendingDispatches = new ConcurrentHashMap<>();

    /** Inicializa o client de imagens com credenciais, base URL e limites de payload da OpenAI. */
    public ImageGenerationOpenAiClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            ImageGenerationWorkerProperties properties,
            String apiKey,
            String baseUrl,
            int maxInMemorySizeBytes
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.enabled = StringUtils.hasText(apiKey);
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(Math.max(1, maxInMemorySizeBytes)));
        if (enabled) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = builder.build();
    }

    /** Registra o despacho lógico para permitir que o backend marque o job como processando antes da chamada longa. */
    @Override
    public OpenAiDispatch dispatch(OpenAiRequest request) {
        if (!enabled) {
            throw new StageWorkerException("OpenAI API key is not configured");
        }

        String marketingHubJobId = marketingHubJobId(request);
        String openAiJobId = "image-" + UUID.randomUUID();
        pendingDispatches.put(openAiJobId, new PendingImageDispatch(request));
        log.info(
                "Envio preparado para OpenAI Images API [jobId={}, openAiJobId={}, requestBodyJson={}]",
                marketingHubJobId,
                openAiJobId,
                request.requestBodyJson()
        );
        return new OpenAiDispatch(
                openAiJobId,
                request.prompt(),
                request.schemaJson(),
                request.requestBodyJson(),
                request.promptMarkdownContent(),
                Instant.now()
        );
    }

    /** Executa uma chamada Images API por prompt planejado e consolida a resposta crua em um manifesto único. */
    @Override
    public OpenAiResult<String> awaitResult(OpenAiDispatch dispatch) {
        if (dispatch.openAiJobId() == null) {
            throw new StageWorkerException("Cannot await OpenAI image result without openAiJobId");
        }
        PendingImageDispatch pending = pendingDispatches.remove(dispatch.openAiJobId());
        if (pending == null) {
            throw new StageWorkerException("OpenAI image dispatch not found in local cache: " + dispatch.openAiJobId());
        }

        OpenAiRequest request = pending.request();
        String marketingHubJobId = marketingHubJobId(request);
        try {
            Map<String, Object> requestBody = objectMapper.readValue(request.requestBodyJson(), new TypeReference<>() {});
            String model = asString(requestBody.get("model"));
            String responseFormat = asString(requestBody.get("responseFormat"));
            List<Map<String, Object>> images = asMapList(requestBody.get("images"));
            if (images.isEmpty()) {
                throw new StageWorkerException("GeraLanding image request has no image prompts");
            }

            List<Map<String, Object>> generated = new ArrayList<>();
            Integer totalInputTokens = null;
            Integer totalOutputTokens = null;
            for (Map<String, Object> image : images) {
                Map<String, Object> openAiBody = buildOpenAiImageBody(model, asString(image.get("prompt")), responseFormat);
                String openAiBodyJson = objectMapper.writeValueAsString(openAiBody);
                log.info(
                        "Envio cru para OpenAI Images API [jobId={}, openAiJobId={}, planningItemKey={}, requestBodyJson={}]",
                        marketingHubJobId,
                        dispatch.openAiJobId(),
                        image.get("planningItemKey"),
                        openAiBodyJson
                );

                JsonNode raw = webClient.post()
                        .uri("/images/generations")
                        .bodyValue(openAiBody)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(properties.timeout());
                if (raw == null) {
                    throw new StageWorkerException("OpenAI returned an empty image generation response");
                }

                String rawJson = objectMapper.writeValueAsString(raw);
                log.info(
                        "Resposta crua recebida da OpenAI Images API [jobId={}, openAiJobId={}, planningItemKey={}, rawResponseJson={}]",
                        marketingHubJobId,
                        dispatch.openAiJobId(),
                        image.get("planningItemKey"),
                        rawJson
                );

                Map<String, Object> generatedImage = new LinkedHashMap<>(image);
                generatedImage.put("model", model);
                generatedImage.put("rawResponse", objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {}));
                generated.add(generatedImage);
                totalInputTokens = addNullable(totalInputTokens, extractUsageToken(raw, "input_tokens"));
                totalOutputTokens = addNullable(totalOutputTokens, extractUsageToken(raw, "output_tokens"));
            }

            String rawJson = buildConsolidatedRawResponse(request, generated);
            return new OpenAiResult<>(
                    dispatch.openAiJobId(),
                    rawJson,
                    rawJson,
                    rawJson,
                    totalInputTokens,
                    totalOutputTokens,
                    BigDecimal.valueOf(Math.max(0d, properties.costPerImageUsd()) * generated.size())
            );
        } catch (WebClientResponseException error) {
            log.error(
                    "Falha HTTP na OpenAI Images API [jobId={}, openAiJobId={}, status={}, responseBody={}, requestBodyJson={}]",
                    marketingHubJobId,
                    dispatch.openAiJobId(),
                    error.getStatusCode().value(),
                    error.getResponseBodyAsString(),
                    request.requestBodyJson(),
                    error
            );
            throw new OpenAiHttpException(error.getStatusCode().value(), error.getResponseBodyAsString(), error);
        } catch (JsonProcessingException error) {
            log.error(
                    "Falha ao preparar request JSON da OpenAI Images API [jobId={}, openAiJobId={}, requestBodyJson={}]",
                    marketingHubJobId,
                    dispatch.openAiJobId(),
                    request.requestBodyJson(),
                    error
            );
            throw new StageWorkerException("Invalid OpenAI image request or response JSON", error);
        }
    }

    /** Monta o payload real da Images API para um único prompt planejado. */
    private Map<String, Object> buildOpenAiImageBody(String model, String prompt, String responseFormat) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        if ("b64_json".equals(responseFormat)) {
            payload.put("response_format", "b64_json");
        }
        return payload;
    }

    /** Serializa a resposta consolidada usada pelo validador e pelo detalhe do GeraLanding. */
    private String buildConsolidatedRawResponse(OpenAiRequest request, List<Map<String, Object>> generated) throws JsonProcessingException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("model", request.model());
        root.put("stageCode", request.metadata().get("stageCode"));
        root.put("idJob", request.metadata().get("idJob"));
        root.put("experimentId", request.metadata().get("experimentId"));
        root.put("images", generated);
        return objectMapper.writeValueAsString(root);
    }

    /** Recupera o idJob do Marketing Hub a partir dos metadados para correlacionar logs operacionais. */
    private String marketingHubJobId(OpenAiRequest request) {
        Object value = request.metadata().get("idJob");
        return value != null ? value.toString() : "<unknown>";
    }

    /** Extrai tokens de uso quando a Images API retorna contabilidade detalhada no payload. */
    private Integer extractUsageToken(JsonNode raw, String fieldName) {
        JsonNode usage = raw.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return null;
        }
        JsonNode direct = usage.path(fieldName);
        if (direct.isNumber()) {
            return direct.intValue();
        }
        JsonNode total = usage.path("total_tokens");
        if ("output_tokens".equals(fieldName) && total.isNumber()) {
            return total.intValue();
        }
        return null;
    }

    /** Soma inteiros que podem estar ausentes preservando nulo quando nenhuma parcela existe. */
    private Integer addNullable(Integer current, Integer increment) {
        if (increment == null) {
            return current;
        }
        return current == null ? increment : current + increment;
    }

    /** Converte lista genérica de objetos JSON em lista de mapas textualmente indexados. */
    private List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> map = new LinkedHashMap<>();
                rawMap.forEach((key, rawValue) -> {
                    if (key != null) {
                        map.put(String.valueOf(key), rawValue);
                    }
                });
                converted.add(map);
            }
        }
        return converted;
    }

    /** Converte valor genérico para texto preservando nulo quando ausente. */
    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Responsabilidade: manter o request lógico enquanto o backend registra o despacho da etapa. */
    private record PendingImageDispatch(OpenAiRequest request) {
        /** Garante que o request pendente exista antes da execução longa na OpenAI. */
        private PendingImageDispatch {
            if (request == null) {
                throw new IllegalArgumentException("request must not be null");
            }
        }
    }
}
