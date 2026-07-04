package com.marketinghub.worker.creative;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Cliente responsável por gerar imagens de criativos usando APIs de imagem da OpenAI.
 */
@Component
public class CreativeImageClient {
    private final WebClient webClient;
    private final BackendAssetClient assetClient;
    private final String model;
    private final String responsesModel;
    private final String serviceTier;
    private final Duration requestTimeout;
    private final CreativeImageOptimizer imageOptimizer;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private static final Logger log = LoggerFactory.getLogger(CreativeImageClient.class);
    private static final int DEFAULT_MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_TRANSIENT_ATTEMPTS = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(15);

    /**
     * Monta o cliente de imagem da OpenAI com timeout e tier compatíveis com Flex.
     */
    public CreativeImageClient(WebClient.Builder builder,
                               BackendAssetClient assetClient,
                               CreativeImageOptimizer imageOptimizer,
                               @Value("${openai.api-key:}") String apiKey,
                               @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                               @Value("${openai.image-model:gpt-image-2}") String model,
                               @Value("${openai.responses-model:gpt-5.5}") String responsesModel,
                               @Value("${openai.image-service-tier:flex}") String serviceTier,
                               @Value("${openai.image-timeout-seconds:900}") long requestTimeoutSeconds) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.requestTimeout = resolveRequestTimeout(requestTimeoutSeconds);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(this.requestTimeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) this.requestTimeout.getSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler((int) this.requestTimeout.getSeconds())));
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_MAX_IN_MEMORY_SIZE));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.assetClient = assetClient;
        this.imageOptimizer = imageOptimizer;
        this.model = model;
        this.responsesModel = normalizeConfig(responsesModel, "gpt-5.5");
        this.serviceTier = normalizeConfig(serviceTier, "flex");
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (!enabled) {
            log.warn("OpenAI API key not configured; creative image generation will fail until the key is configured");
        }
    }

    /**
     * Gera uma imagem para um prompt sem prompt intermediário.
     */
    public String generateImage(String prompt) {
        return generateImage(prompt, null, "creative-default");
    }

    /**
     * Gera uma imagem e retorna uma URL utilizável ou falha quando a geração não puder executar.
     */
    public String generateImage(String prompt, String intermediatePrompt) {
        return generateImage(prompt, intermediatePrompt, "creative");
    }

    /**
     * Gera uma imagem com contexto operacional para isolar falhas de OpenAI, upload ou configuração.
     */
    public String generateImage(String prompt, String intermediatePrompt, String operationContext) {
        String context = normalizeContext(operationContext);
        if (!enabled) {
            log.error("Cannot generate creative image because OpenAI API key is missing. context={}", context);
            throw new IllegalStateException("OpenAI API key is required to generate creative images");
        }
        OpenAiImageResult imageResult = callOpenAiForImage(prompt, context);
        ImageData data = imageResult.data();
        if (data.base64() != null && !data.base64().isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(data.base64());
                CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(imageBytes);
                if (log.isInfoEnabled()) {
                    log.info("Optimized OpenAI image from {} bytes to {} bytes", imageBytes.length,
                            optimized.content().length);
                }
                String filename = "creative-" + UUID.randomUUID() + "." + optimized.extension();
                String uploadedUrl = assetClient.uploadImage(optimized.content(), filename, imageResult.sourceModel(), prompt,
                        intermediatePrompt);
                log.info("Creative image generated by OpenAI and uploaded to backend. context={} filename={} url={}",
                        context, filename, uploadedUrl);
                return uploadedUrl;
            } catch (BackendAssetClient.BackendAssetUploadException e) {
                log.error("OpenAI returned image bytes but backend asset upload failed. context={}", context, e);
                throw e;
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode OpenAI image payload. context={}", context, e);
                throw new RuntimeException("Failed to decode image payload", e);
            }
        }
        if (data.url() != null && !data.url().isBlank()) {
            log.warn("OpenAI image response missing base64 payload, using remote URL. context={} url={}",
                    context, data.url());
            return data.url();
        }
        throw new RuntimeException("No image returned from OpenAI");
    }

    /**
     * Chama o endpoint de imagem da OpenAI e extrai o payload de imagem gerado.
     */
    private OpenAiImageResult callOpenAiForImage(String prompt, String context) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_TRANSIENT_ATTEMPTS; attempt++) {
            String effectiveTier = effectiveServiceTierForAttempt(attempt);
            try {
                return isFlexTier(effectiveTier)
                        ? callResponsesImageTool(prompt, context, effectiveTier, attempt)
                        : callImageApi(prompt, context, effectiveTier, attempt);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (!isTransientOpenAiFailure(ex) || attempt == MAX_TRANSIENT_ATTEMPTS) {
                    throw ex;
                }
                log.warn(
                        "OpenAI creative image generation failed with transient error. context={} attempt={} nextAttemptTier={} error={}",
                        context,
                        attempt,
                        effectiveServiceTierForAttempt(attempt + 1),
                        ex.getMessage());
            }
        }
        throw lastFailure != null ? lastFailure : new RuntimeException("No image returned from OpenAI");
    }

    /**
     * Chama a ferramenta de imagem da Responses API usando o tier efetivo da tentativa.
     */
    private OpenAiImageResult callResponsesImageTool(String prompt, String context, String effectiveTier, int attempt) {
        Map<String, Object> imageTool = new LinkedHashMap<>();
        imageTool.put("type", "image_generation");
        imageTool.put("action", "generate");
        imageTool.put("model", model);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", responsesModel);
        payload.put("input", prompt);
        payload.put("tools", List.of(imageTool));
        payload.put("service_tier", effectiveTier);

        log.info(
                "Sending creative image request to OpenAI Responses API. context={} attempt={} timeoutSeconds={} payload={}",
                context,
                attempt,
                requestTimeout.getSeconds(),
                payload);
        ResponsesImageResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .exchangeToMono(clientResponse -> readResponsesImageResponse(clientResponse, context))
                    .block(requestTimeout);
        } catch (Exception ex) {
            log.error("OpenAI Responses image generation transport failed after {} seconds. context={}",
                    requestTimeout.getSeconds(), context, ex);
            throw new RuntimeException("Failed to call OpenAI Responses API", ex);
        }

        log.info("Parsed OpenAI Responses image response. context={} parsedResponse={}", context, response);
        return extractResponsesImage(response);
    }

    /**
     * Chama a Image API direta para modos de geração que não usam Flex.
     */
    private OpenAiImageResult callImageApi(String prompt, String context, String effectiveTier, int attempt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        if (supportsResponseFormat(model)) {
            payload.put("response_format", "b64_json");
        }

        log.info(
                "Sending creative image request to OpenAI Image API. context={} attempt={} serviceTier={} timeoutSeconds={} payload={}",
                context,
                attempt,
                effectiveTier,
                requestTimeout.getSeconds(),
                payload);
        ImageResponse response;
        try {
            response = webClient.post()
                    .uri("/images/generations")
                    .bodyValue(payload)
                    .exchangeToMono(clientResponse -> readImageResponse(clientResponse, context))
                    .block(requestTimeout);
        } catch (Exception ex) {
            log.error("OpenAI image generation transport failed after {} seconds. context={}",
                    requestTimeout.getSeconds(), context, ex);
            throw new RuntimeException("Failed to call OpenAI image API", ex);
        }

        log.info("Parsed OpenAI image response. context={} parsedResponse={}", context, response);
        return extractImageApiImage(response);
    }

    /**
     * Extrai a primeira imagem gerada de uma resposta da Image API.
     */
    private OpenAiImageResult extractImageApiImage(ImageResponse response) {
        if (response == null) {
            throw new RuntimeException("No image returned from OpenAI");
        }
        List<ImageData> dataList = response.data();
        if (dataList == null || dataList.isEmpty()) {
            ApiError error = response.error();
            if (error != null && error.message() != null && !error.message().isBlank()) {
                log.error("OpenAI image API returned error. message={} type={} param={} code={}",
                        error.message(), error.type(), error.param(), error.code());
                throw new RuntimeException("OpenAI image API error: " + error.message());
            }
            throw new RuntimeException("No image returned from OpenAI");
        }
        return new OpenAiImageResult(dataList.get(0), model);
    }

    /**
     * Extrai a primeira imagem gerada de uma resposta da ferramenta de imagem da Responses API.
     */
    private OpenAiImageResult extractResponsesImage(ResponsesImageResponse response) {
        if (response == null) {
            throw new RuntimeException("No image returned from OpenAI Responses API");
        }
        ApiError error = response.error();
        if (error != null && error.message() != null && !error.message().isBlank()) {
            log.error("OpenAI Responses API returned error. message={} type={} param={} code={}",
                    error.message(), error.type(), error.param(), error.code());
            throw new RuntimeException("OpenAI Responses API error: " + error.message());
        }
        if (response.output() != null) {
            for (ResponsesOutput output : response.output()) {
                if (output != null && "image_generation_call".equals(output.type())
                        && output.result() != null && !output.result().isBlank()) {
                    return new OpenAiImageResult(new ImageData(null, output.result()), model);
                }
            }
        }
        throw new RuntimeException("No image returned from OpenAI Responses API");
    }

    /**
     * Lê e registra a resposta bruta da OpenAI antes de interpretar o contrato de imagem.
     */
    private Mono<ImageResponse> readImageResponse(ClientResponse response, String context) {
        HttpStatusCode status = response.statusCode();
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, this::appendChunk)
                .map(ByteArrayOutputStream::toByteArray)
                .map(bytes -> parseResponse(bytes, status, context));
    }

    /**
     * Copia um buffer reativo para array de bytes e libera o buffer.
     */
    private byte[] toByteArray(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    /**
     * Adiciona um trecho de resposta respeitando o limite de memória configurado.
     */
    private ByteArrayOutputStream appendChunk(ByteArrayOutputStream output, byte[] chunk) {
        if (output.size() + chunk.length > DEFAULT_MAX_IN_MEMORY_SIZE) {
            throw new IllegalStateException("OpenAI image payload exceeds allowed size");
        }
        output.write(chunk, 0, chunk.length);
        return output;
    }

    /**
     * Lê e registra a resposta bruta da Responses API antes de interpretar o contrato da ferramenta de imagem.
     */
    private Mono<ResponsesImageResponse> readResponsesImageResponse(ClientResponse response, String context) {
        HttpStatusCode status = response.statusCode();
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, this::appendChunk)
                .map(ByteArrayOutputStream::toByteArray)
                .map(bytes -> parseResponsesImageResponse(bytes, status, context));
    }

    /**
     * Interpreta a resposta bruta de imagem após registrar status e corpo para análise de causa-raiz.
     */
    private ImageResponse parseResponse(byte[] bytes, HttpStatusCode status, String context) {
        String rawResponse = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received raw OpenAI image response. context={} status={} rawResponse={}",
                context, status.value(), rawResponse);
        try {
            return objectMapper.readValue(bytes, ImageResponse.class);
        } catch (IOException e) {
            log.error("Failed to decode OpenAI image response as JSON. context={} status={} rawResponse={}",
                    context, status.value(), rawResponse, e);
            throw new RuntimeException("Failed to decode image payload", e);
        }
    }

    /**
     * Interpreta a resposta bruta da Responses API após registrar status e corpo para causa-raiz.
     */
    private ResponsesImageResponse parseResponsesImageResponse(byte[] bytes, HttpStatusCode status, String context) {
        String rawResponse = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received raw OpenAI Responses image response. context={} status={} rawResponse={}",
                context, status.value(), rawResponse);
        try {
            return objectMapper.readValue(bytes, ResponsesImageResponse.class);
        } catch (IOException e) {
            log.error("Failed to decode OpenAI Responses image response as JSON. context={} status={} rawResponse={}",
                    context, status.value(), rawResponse, e);
            throw new RuntimeException("Failed to decode image payload", e);
        }
    }

    /**
     * Indica se o modelo selecionado aceita campo explícito de response_format.
     */
    private boolean supportsResponseFormat(String selectedModel) {
        if (selectedModel == null || selectedModel.isBlank()) {
            return true;
        }
        return !selectedModel.toLowerCase(Locale.ROOT).startsWith("gpt-image-");
    }

    /**
     * Determina se o tier configurado deve usar o caminho Flex da Responses API.
     */
    private boolean isFlexTier(String selectedServiceTier) {
        return "flex".equalsIgnoreCase(selectedServiceTier);
    }

    /**
     * Mantém as duas primeiras tentativas em Flex e usa Standard na terceira para erro transitório.
     */
    private String effectiveServiceTierForAttempt(int attempt) {
        if (!isFlexTier(serviceTier)) {
            return serviceTier;
        }
        return attempt >= MAX_TRANSIENT_ATTEMPTS ? "default" : "flex";
    }

    /**
     * Identifica falhas transitórias da OpenAI que merecem nova tentativa em outro tier.
     */
    private boolean isTransientOpenAiFailure(RuntimeException ex) {
        StringBuilder details = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null) {
                details.append(' ').append(current.getMessage());
            }
            details.append(' ').append(current.getClass().getName());
            current = current.getCause();
        }
        String normalized = details.toString().toLowerCase(Locale.ROOT);
        return normalized.contains("429")
                || normalized.contains("408")
                || normalized.contains("5xx")
                || normalized.contains("rate_limit")
                || normalized.contains("too many requests")
                || normalized.contains("temporarily unavailable")
                || normalized.contains("timeout")
                || normalized.contains("connection prematurely closed")
                || normalized.contains("prematurecloseexception")
                || normalized.contains("connection reset")
                || normalized.contains("webclientrequestexception");
    }

    /**
     * Resolve o timeout da requisição mantendo padrão compatível com Flex para configuração inválida.
     */
    private Duration resolveRequestTimeout(long requestTimeoutSeconds) {
        if (requestTimeoutSeconds <= 0) {
            return DEFAULT_REQUEST_TIMEOUT;
        }
        return Duration.ofSeconds(requestTimeoutSeconds);
    }

    /**
     * Normaliza valores de configuração vazios para padrões operacionais explícitos.
     */
    private String normalizeConfig(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Normaliza contexto ausente para manter logs pesquisáveis.
     */
    private String normalizeContext(String operationContext) {
        if (operationContext == null || operationContext.isBlank()) {
            return "creative";
        }
        return operationContext.trim();
    }

    private record OpenAiImageResult(ImageData data, String sourceModel) {}
    private record ImageResponse(List<ImageData> data, ApiError error) {}
    private record ResponsesImageResponse(List<ResponsesOutput> output, ApiError error) {}
    private record ResponsesOutput(String type, String result) {}
    private record ImageData(String url, @JsonProperty("b64_json") String base64) {}
    private record ApiError(String message, String type, String param, String code) {}
}
