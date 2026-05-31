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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Responsabilidade: adaptar a Images API da OpenAI ao contrato genérico OpenAiClientPort do core. */
public class ImageGenerationOpenAiClient implements OpenAiClientPort {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationOpenAiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ImageGenerationWorkerProperties properties;
    private final boolean enabled;
    private final Map<String, OpenAiResult<String>> resultCache = new ConcurrentHashMap<>();

    /** Inicializa o cliente de imagens com WebClient, ObjectMapper, propriedades e credenciais OpenAI. */
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

    /** Envia uma requisição síncrona para a OpenAI Images API e guarda a resposta para o awaitResult do core. */
    @Override
    public OpenAiDispatch dispatch(OpenAiRequest request) {
        if (!enabled) {
            throw new StageWorkerException("OpenAI API key is not configured");
        }

        String marketingHubJobId = marketingHubJobId(request);
        String requestBodyJson = request.requestBodyJson();
        try {
            Map<String, Object> requestBody = objectMapper.readValue(requestBodyJson, new TypeReference<>() {});
            log.info(
                    "Envio cru para OpenAI Images API [jobId={}, requestBodyJson={}]",
                    marketingHubJobId,
                    requestBodyJson
            );

            JsonNode raw = webClient.post()
                    .uri("/images/generations")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(properties.timeout());

            if (raw == null) {
                throw new StageWorkerException("OpenAI returned an empty image generation response");
            }

            String rawJson = objectMapper.writeValueAsString(raw);
            log.info(
                    "Resposta crua recebida da OpenAI Images API [jobId={}, rawResponseJson={}]",
                    marketingHubJobId,
                    rawJson
            );
            String openAiJobId = "image-" + UUID.randomUUID();
            OpenAiResult<String> result = new OpenAiResult<>(
                    openAiJobId,
                    rawJson,
                    rawJson,
                    rawJson,
                    extractUsageToken(raw, "input_tokens"),
                    extractUsageToken(raw, "output_tokens"),
                    BigDecimal.valueOf(Math.max(0d, properties.costPerImageUsd()))
            );
            resultCache.put(openAiJobId, result);
            return new OpenAiDispatch(
                    openAiJobId,
                    request.prompt(),
                    request.schemaJson(),
                    requestBodyJson,
                    request.promptMarkdownContent(),
                    Instant.now()
            );
        } catch (WebClientResponseException error) {
            log.error(
                    "Falha HTTP na OpenAI Images API [jobId={}, status={}, responseBody={}, requestBodyJson={}]",
                    marketingHubJobId,
                    error.getStatusCode().value(),
                    error.getResponseBodyAsString(),
                    requestBodyJson,
                    error
            );
            throw new OpenAiHttpException(error.getStatusCode().value(), error.getResponseBodyAsString(), error);
        } catch (JsonProcessingException error) {
            log.error(
                    "Falha ao preparar request JSON da OpenAI Images API [jobId={}, requestBodyJson={}]",
                    marketingHubJobId,
                    requestBodyJson,
                    error
            );
            throw new StageWorkerException("Invalid OpenAI image request or response JSON", error);
        }
    }

    /** Recupera a resposta bruta da Images API armazenada localmente após o despacho síncrono. */
    @Override
    public OpenAiResult<String> awaitResult(OpenAiDispatch dispatch) {
        if (dispatch.openAiJobId() == null) {
            throw new StageWorkerException("Cannot await OpenAI image result without openAiJobId");
        }
        OpenAiResult<String> result = resultCache.remove(dispatch.openAiJobId());
        if (result == null) {
            throw new StageWorkerException("OpenAI image result not found in local cache: " + dispatch.openAiJobId());
        }
        return result;
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
}
